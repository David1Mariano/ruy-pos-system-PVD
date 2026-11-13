package br.com.ruy.pos.payment

import androidx.room.withTransaction
import br.com.ruy.pos.data.local.*
import java.util.UUID
import org.json.JSONObject

/** All money/table transitions share ONE Room transaction. No callback can settle money. */
class PaymentCoordinator(private val db: AppDatabase, private val options: () -> ExternalPaymentOptions = { ExternalPaymentOptions() }) {
    fun observeActive() = db.stoneDao().observeActive()
    fun observeAttempts(orderId: String) = db.stoneDao().observeByOrder(orderId)
    fun observePayments(orderId: String) = db.paymentDao().observe(orderId)
    fun observeOrder(orderId: String) = db.orderDao().observe(orderId)

    private suspend fun requireNoFlight() {
        check(db.stoneDao().active() == null) { "Há tentativa pendente/indeterminada. Concilie antes de cobrar novamente" }
        check(db.paymentDao().getAll().none { it.status in setOf("AGUARDANDO", "INDETERMINADO", "EM_CONCILIACAO", "CRIADA") }) {
            "Há pagamento anterior pendente. Consulte o pedido antes de cobrar novamente"
        }
    }
    private suspend fun remaining(order: OrderEntity): Int {
        check(order.paymentStatus != "CANCELADO") { "Pedido cancelado" }
        val paid = db.paymentDao().getByOrder(order.id).filter { it.status == "PAGO" }.sumOf { it.amountCents.toLong() }
        return (order.totalCents.toLong() - paid).coerceAtLeast(0).toInt()
    }

    suspend fun createAttempt(orderId: String, amountCents: Int, method: String, installments: Int = 1,
                              installmentType: String = "NONE"): StoneAttemptEntity = db.withTransaction {
        requireNoFlight()
        val order = requireNotNull(db.orderDao().byId(orderId)) { "Pedido não encontrado" }
        require(order.paymentStatus != "PAGO" && amountCents in 1..remaining(order)) { "Valor excede saldo do pedido" }
        verifyComanda(order)
        val session = requireNotNull(db.cashSessionDao().getOpen()) { "Abra o Caixa antes de iniciar pagamento" }
        StoneProtocol.validate(PaymentRequest(orderId, amountCents, method, installments, "1", installmentType))
        val payment = PaymentEntity(UUID.randomUUID().toString(), orderId, amountCents,
            requireNotNull(StoneProtocol.method(method)), "CRIADA", installments = installments, cashSessionId = session.sessionId)
        db.paymentDao().insert(payment)
        val attempt = StoneAttemptEntity(attemptId = UUID.randomUUID().toString(), paymentId = payment.paymentId,
            orderId = orderId, installmentType = installmentType)
        val reference = db.stoneDao().insert(attempt)
        check(reference > 0) { "Referência Stone esgotada" }
        db.orderDao().update(order.copy(paymentStatus = "PENDENTE", syncStatus = if (br.com.ruy.pos.BuildConfig.HOMOLOGATION) "HOMOLOGATION" else "PENDING"))
        attempt.copy(stoneOrderId = reference)
    }

    suspend fun request(attempt: StoneAttemptEntity): PaymentRequest {
        val p = requireNotNull(db.paymentDao().byId(attempt.paymentId))
        check(p.orderId == attempt.orderId)
        return PaymentRequest(p.orderId, p.amountCents, p.method, p.installments, attempt.stoneOrderId.toString(), attempt.installmentType)
    }

    /** Recover payments made by older app versions without launching another charge or guessing table links. */
    suspend fun recoverLegacyPayment(paymentId: String): StoneAttemptEntity = db.withTransaction {
        check(db.stoneDao().active() == null) { "Concilie a tentativa ativa primeiro" }
        val payment = requireNotNull(db.paymentDao().byId(paymentId))
        check(payment.status in setOf("AGUARDANDO", "INDETERMINADO", "EM_CONCILIACAO", "CRIADA") &&
            db.stoneDao().byPayment(paymentId) == null) { "Pagamento não é uma pendência anterior sem tentativa" }
        val order = requireNotNull(db.orderDao().byId(payment.orderId))
        require(payment.amountCents > 0 && payment.amountCents <= remaining(order))
        val session = requireNotNull(db.cashSessionDao().getOpen()) { "Abra o Caixa para regularizar o pagamento anterior" }
        val method = requireNotNull(StoneProtocol.method(payment.method)) { "Modalidade anterior não suportada" }
        db.paymentDao().update(payment.copy(method = method, status = "INDETERMINADO", cashSessionId = session.sessionId))
        val a = StoneAttemptEntity(attemptId = UUID.randomUUID().toString(), paymentId = paymentId, orderId = order.id,
            installmentType = if (payment.installments > 1) "NAO_INFORMADO" else "NONE", origin = "LEGACY_MANUAL", state = "INDETERMINADO",
            detail = "Pagamento anterior sem retorno confiável. Referência local nunca enviada à Stone. Conciliação contabilizada no Caixa atualmente aberto; vínculos antigos de mesa não são presumidos")
        a.copy(stoneOrderId = db.stoneDao().insert(a))
    }

    suspend fun claimLaunch(reference: Long): Boolean = db.withTransaction {
        val attempt = requireNotNull(db.stoneDao().byReference(reference))
        if (db.stoneDao().claimLaunch(reference, System.currentTimeMillis()) != 1) return@withTransaction false
        val payment = requireNotNull(db.paymentDao().byId(attempt.paymentId))
        db.paymentDao().update(payment.copy(status = "AGUARDANDO"))
        true
    }

    suspend fun launchFailed(reference: Long, definitelyNotStarted: Boolean) = db.withTransaction {
        val attempt = requireNotNull(db.stoneDao().byReference(reference))
        // A callback may already have arrived. Never overwrite its evidence or release its lock.
        if (attempt.state != "AGUARDANDO" || attempt.activeSlot == null) return@withTransaction
        val state = if (definitelyNotStarted) "NAO_INICIADO" else "INDETERMINADO"
        db.stoneDao().update(attempt.copy(state = state, activeSlot = if (definitelyNotStarted) null else 1,
            detail = if (definitelyNotStarted) "Falha de abertura; nenhuma atividade iniciada" else "Abertura inconclusiva; conferir extrato",
            updatedAt = System.currentTimeMillis()))
        val p = requireNotNull(db.paymentDao().byId(attempt.paymentId))
        db.paymentDao().update(p.copy(status = state))
    }

    suspend fun receive(raw: String): String {
        val callback = try { StoneProtocol.parse(raw) } catch (e: Exception) {
            db.stoneDao().event(StoneCallbackEventEntity(StoneProtocol.fingerprint(raw), null, "RETORNO_INVALIDO"))
            return "Retorno inválido ignorado. Consulte pagamentos pendentes"
        }
        return db.withTransaction {
            val attempt = callback.reference?.let { db.stoneDao().byReference(it) }
                ?: if (callback.fields["order_id"] == null) db.stoneDao().active() else null
            if (db.stoneDao().event(StoneCallbackEventEntity(callback.fingerprint, attempt?.attemptId,
                    if (attempt == null) "DESCONHECIDO" else "RECEBIDO", JSONObject(callback.fields).toString())) == -1L) return@withTransaction "Retorno duplicado ignorado"
            if (attempt == null) return@withTransaction "Retorno de tentativa desconhecida; nenhuma baixa realizada"
            if (attempt.origin in setOf("TAPSTONE_EXTERNAL", "PIX_STATIC")) return@withTransaction "Pagamento externo exige conferência manual; retorno ignorado"
            if (attempt.activeSlot == null) return@withTransaction "Tentativa já encerrada; retorno auditado sem nova baixa"
            val p = db.paymentDao().byId(attempt.paymentId)
            val order = db.orderDao().byId(attempt.orderId)
            if (p == null || order == null || p.orderId != order.id) return@withTransaction "Vínculo inválido; conciliação necessária"
            val assessment = StoneProtocol.assess(callback, request(attempt))
            val atk = callback.fields["atk"]?.takeIf { it.isNotBlank() }
            val conflict = atk != null && (db.stoneDao().otherWithAtk(atk, attempt.stoneOrderId) != null ||
                (attempt.atk != null && attempt.atk != atk))
            val state = if (conflict) "INDETERMINADO" else assessment.state
            db.stoneDao().update(attempt.copy(state = state, atk = if (conflict) attempt.atk else atk ?: attempt.atk,
                itk = callback.fields["itk"] ?: attempt.itk,
                authorizationCode = callback.fields["authorization_code"] ?: attempt.authorizationCode,
                authorizationDateTime = callback.fields["authorization_date_time"] ?: attempt.authorizationDateTime,
                returnCode = callback.fields["code"], detail = if (conflict) "ATK conflitante; conferir extrato" else assessment.detail,
                updatedAt = System.currentTimeMillis()))
            // RECUSADO/CANCELADO are reported outcomes, still locked until checked in the Stone statement.
            db.paymentDao().update(p.copy(status = if (state in setOf("RECUSADO", "CANCELADO")) state else "INDETERMINADO"))
            "${if (conflict) "ATK conflitante" else assessment.detail}. Sem baixa automática"
        }
    }

    suspend fun expireMissingReturn(now: Long = System.currentTimeMillis()) = db.withTransaction {
        val a = db.stoneDao().active() ?: return@withTransaction
        if (a.state !in setOf("CRIADA", "AGUARDANDO") || now - a.createdAt < StoneProtocol.RETURN_TIMEOUT_MS) return@withTransaction
        db.stoneDao().update(a.copy(state = "INDETERMINADO", detail = "Sem retorno ou app interrompido; conferir extrato antes de repetir", updatedAt = now))
        val p = requireNotNull(db.paymentDao().byId(a.paymentId))
        db.paymentDao().update(p.copy(status = "INDETERMINADO"))
    }

    data class Reconciliation(
        val reference: Long,
        val amountCents: Int,
        val method: String,
        val installments: Int,
        val received: Boolean,
        val operator: String,
        val evidence: String,
        val checkedStatement: Boolean,
        val atk: String = "",
        val authorizationCode: String = ""
    )

    /** Explicit operator attestation against Stone receipt/statement, NOT an online Stone API verification. */
    suspend fun reconcile(input: Reconciliation) = db.withTransaction {
        br.com.ruy.pos.security.PaymentAudit.validate(input.operator, input.evidence, input.atk, input.authorizationCode)
        require(input.checkedStatement && input.operator.trim().length >= 3 && input.evidence.trim().length >= 8) {
            "Informe operador e comprovante/extrato consultado e confirme a conferência no terminal Stone"
        }
        val a = requireNotNull(db.stoneDao().byReference(input.reference)) { "Tentativa não encontrada" }
        val p = requireNotNull(db.paymentDao().byId(a.paymentId))
        require(input.amountCents == p.amountCents && StoneProtocol.method(input.method) == p.method && input.installments == p.installments) {
            "Valor, modalidade ou parcelas divergem da tentativa"
        }
        if (a.activeSlot == null) {
            check((a.state == "CONFIRMADO") == input.received && (!input.received || a.atk == input.atk.trim())) {
                "Conciliação conflitante com resultado já encerrado"
            }
            return@withTransaction
        }
        if (input.received) {
            val atk = input.atk.trim()
            require(atk.matches(Regex("[A-Za-z0-9._-]{3,128}"))) { "Informe ATK/Stone ID do comprovante Stone" }
            require(p.method in setOf("PIX", "INSTANT_PAYMENT") || input.authorizationCode.trim().length in 3..32) {
                "Informe código de autorização do comprovante"
            }
            check(db.stoneDao().otherWithAtk(atk, a.stoneOrderId) == null &&
                db.paymentDao().getAll().none { it.paymentId != p.paymentId && it.status == "PAGO" && it.transactionRef.equals(atk, ignoreCase = true) }) {
                "ATK já vinculado a outro pagamento"
            }
            // Independent receipt data may resolve missing/divergent callback evidence, with operator audit.
            settle(p.copy(transactionRef = atk))
            db.stoneDao().update(a.copy(state = "CONFIRMADO", activeSlot = null, atk = atk,
                authorizationCode = input.authorizationCode.trim().ifEmpty { null },
                reconciledBy = input.operator.trim(), reconciliationEvidence = "MANUAL_COMPROVANTE/${a.origin}: ${input.evidence.trim()}",
                detail = "Recebimento conciliado pelo operador no comprovante/extrato Stone", updatedAt = System.currentTimeMillis()))
        } else {
            db.paymentDao().update(p.copy(status = "SEM_RECEBIMENTO"))
            db.stoneDao().update(a.copy(state = "SEM_RECEBIMENTO", activeSlot = null, reconciledBy = input.operator.trim(),
                reconciliationEvidence = "MANUAL_EXTRATO_SEM_RECEBIMENTO: ${input.evidence.trim()}",
                detail = "Operador confirmou ausência de recebimento e de transação em andamento", updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun confirmCash(orderId: String, amountCents: Int, receivedCents: Int): String = db.withTransaction {
        requireNoFlight()
        val order = requireNotNull(db.orderDao().byId(orderId))
        require(amountCents in 1..remaining(order) && receivedCents >= amountCents) { "Valor em dinheiro inválido ou pedido quitado" }
        val session = requireNotNull(db.cashSessionDao().getOpen()) { "Abra o Caixa antes de receber" }
        val p = PaymentEntity(UUID.randomUUID().toString(), orderId, amountCents, "DINHEIRO", "CRIADA",
            receivedCents, receivedCents - amountCents, cashSessionId = session.sessionId)
        db.paymentDao().insert(p)
        settle(p)
        p.paymentId
    }

    /** Payment executed on an EXTERNAL provider/machine (other POS, bank app, Pix manual). */
    suspend fun confirmExternal(orderId: String, amountCents: Int, method: String, operator: String,
                                evidence: String, installments: Int = 1, transactionRef: String? = null, operationId: String? = null): String = db.withTransaction {
        br.com.ruy.pos.security.PaymentAudit.validate(operator, evidence, transactionRef.orEmpty())
        val normalized = if (method in ExternalPaymentOptions.VOUCHER_METHODS) method else requireNotNull(StoneProtocol.method(method)) { "Modalidade externa inválida" }
        require(normalized in setOf("PIX", "DEBIT", "CREDIT") + ExternalPaymentOptions.VOUCHER_METHODS)
        val voucher = if (normalized in ExternalPaymentOptions.VOUCHER_METHODS) requireNotNull(options().validated().vouchers.find { it.method == normalized }) { "Voucher não habilitado para o estabelecimento" } else null
        require(installments in 1..99 && (normalized == "CREDIT" || installments == 1))
        require(operator.trim().length >= 3) { "Informe o operador responsável (3 ou mais caracteres)" }
        require(evidence.trim().length >= 8) { "Informe comprovante/extrato consultado (8 ou mais caracteres)" }
        require(operationId == null || operationId.matches(Regex("[0-9a-f-]{36}"))) { "Confirmação inválida" }
        val ref = transactionRef?.trim()?.takeIf { it.isNotEmpty() }
        require(ref != null && ref.matches(Regex("[A-Za-z0-9._-]{3,128}"))) { "Informe a referência do comprovante aprovado; não informe dados de cartão" }
        operationId?.let { id -> db.stoneDao().byAttemptId(id)?.let { a ->
            val existing = requireNotNull(db.paymentDao().byId(a.paymentId))
            check(a.origin == "EXTERNAL_MANUAL" && existing.orderId == orderId && existing.amountCents == amountCents && existing.method == normalized && existing.installments == installments && existing.transactionRef.equals(ref, ignoreCase = true)) { "Confirmação conflitante" }
            return@withTransaction existing.paymentId
        } }
        requireNoFlight()
        val order = requireNotNull(db.orderDao().byId(orderId)) { "Pedido não encontrado" }
        require(amountCents in 1..remaining(order)) { "Valor excede o saldo do pedido" }
        val session = requireNotNull(db.cashSessionDao().getOpen()) { "Abra o Caixa antes de registrar recebimento" }
        check(db.paymentDao().getAll().none { it.status == "PAGO" && it.transactionRef.equals(ref, ignoreCase = true) }) { "Comprovante já utilizado" }
        val p = PaymentEntity(UUID.randomUUID().toString(), orderId, amountCents, normalized, "PAGO_MANUALMENTE",
            null, null, installments, ref, cashSessionId = session.sessionId)
        db.paymentDao().insert(p)
        settle(p)
        db.stoneDao().insert(StoneAttemptEntity(attemptId = operationId ?: UUID.randomUUID().toString(), paymentId = p.paymentId,
            orderId = orderId, installmentType = "NONE", origin = "EXTERNAL_MANUAL", state = "CONFIRMADO", activeSlot = null,
            reconciledBy = operator.trim(), reconciliationEvidence = evidence.trim(), detail = if (voucher == null) "PAGO_MANUALMENTE; provedor externo" else JSONObject().put("provider", voucher.provider).put("brand", voucher.brand).toString()))
        p.paymentId
    }

    /** Saves intent to collect externally; never invokes a financial app or network. */
    suspend fun beginTapStone(orderId: String, amountCents: Int, method: String, installments: Int = 1): StoneAttemptEntity = db.withTransaction {
        requireNoFlight()
        require(method in setOf("TAPSTONE_DEBIT", "TAPSTONE_CREDIT"))
        require(installments in 1..18 && (method == "TAPSTONE_CREDIT" || installments == 1)) { "Parcelas não disponíveis para esta modalidade" }
        val order = requireNotNull(db.orderDao().byId(orderId))
        require(amountCents in 1..remaining(order)) { "Valor excede saldo" }
        verifyComanda(order)
        val session = requireNotNull(db.cashSessionDao().getOpen()) { "Abra o Caixa" }
        val p = PaymentEntity(UUID.randomUUID().toString(), orderId, amountCents, method, "AGUARDANDO", installments = installments, cashSessionId = session.sessionId)
        db.paymentDao().insert(p)
        val a = StoneAttemptEntity(attemptId = UUID.randomUUID().toString(), paymentId = p.paymentId, orderId = orderId,
            installmentType = "NONE", origin = "TAPSTONE_EXTERNAL", state = "EM_CONFERENCIA",
            detail = "Cobrança somente no aplicativo oficial Stone. Sem confirmação automática; referência local não enviada à Stone.")
        db.orderDao().update(order.copy(paymentStatus = "PENDENTE", syncStatus = if (br.com.ruy.pos.BuildConfig.HOMOLOGATION) "HOMOLOGATION" else "PENDING"))
        a.copy(stoneOrderId = db.stoneDao().insert(a))
    }

    data class TapStoneProof(val reference: Long, val orderId: String, val amountCents: Int, val method: String,
        val received: Boolean, val operator: String, val evidence: String, val receipt: String, val checked: Boolean, val installments: Int = 1)

    suspend fun confirmTapStone(input: TapStoneProof) = db.withTransaction {
        br.com.ruy.pos.security.PaymentAudit.validate(input.operator, input.evidence, input.receipt)
        require(input.checked && input.operator.trim().length in 3..80 && input.evidence.trim().length in 8..500) {
            "Confira o comprovante/extrato e informe operador e evidência; não informe dados de cartão"
        }
        val a = requireNotNull(db.stoneDao().byReference(input.reference))
        check(a.origin == "TAPSTONE_EXTERNAL")
        val p = requireNotNull(db.paymentDao().byId(a.paymentId))
        require(input.orderId == p.orderId && input.amountCents == p.amountCents && input.method == p.method && input.installments == p.installments) {
            "Pedido, valor ou modalidade divergente do comprovante"
        }
        val receipt = input.receipt.trim()
        if (a.activeSlot == null) {
            check((a.state == "CONFIRMADO") == input.received && (!input.received || p.transactionRef.equals(receipt, ignoreCase = true))) { "Confirmação conflitante" }
            return@withTransaction
        }
        if (input.received) {
            require(receipt.matches(Regex("[A-Za-z0-9._-]{3,128}"))) { "Informe referência do comprovante aprovado; nunca número do cartão" }
            check(db.paymentDao().getAll().none { it.paymentId != p.paymentId && it.status == "PAGO" && it.transactionRef.equals(receipt, ignoreCase = true) }) { "Comprovante já utilizado" }
            settle(p.copy(transactionRef = receipt))
        } else db.paymentDao().update(p.copy(status = "SEM_RECEBIMENTO"))
        db.stoneDao().update(a.copy(state = if (input.received) "CONFIRMADO" else "SEM_RECEBIMENTO", activeSlot = null,
            reconciledBy = input.operator.trim(), reconciliationEvidence = "MANUAL_TAPSTONE: ${input.evidence.trim()}",
            detail = if (input.received) "PAGO_MANUALMENTE após conferência Stone" else "Extrato conferido: sem recebimento e sem transação em andamento",
            updatedAt = System.currentTimeMillis()))
    }

    suspend fun beginPix(orderId: String, quote: br.com.ruy.pos.payment.pix.PixQuote, attemptId: String): StoneAttemptEntity = db.withTransaction {
        requireNoFlight()
        val parsed = br.com.ruy.pos.payment.pix.PixBrCode.validate(quote.payload)
        require(parsed.amountCents == quote.amountCents && parsed.txid == quote.txid && parsed.txid != "***" && parsed.receiver == quote.receiver)
        require(attemptId.matches(Regex("[0-9a-f-]{36}")))
        val order = requireNotNull(db.orderDao().byId(orderId))
        require(quote.amountCents in 1..remaining(order)) { "Valor excede saldo" }
        verifyComanda(order)
        val session = requireNotNull(db.cashSessionDao().getOpen()) { "Abra o Caixa" }
        val p = PaymentEntity(UUID.randomUUID().toString(), orderId, quote.amountCents, "PIX_STATIC", "AGUARDANDO", cashSessionId = session.sessionId)
        db.paymentDao().insert(p)
        val a = StoneAttemptEntity(attemptId = attemptId, paymentId = p.paymentId, orderId = orderId, installmentType = "NONE",
            origin = "PIX_STATIC", state = "AGUARDANDO_PAGAMENTO", detail = JSONObject().put("txid", quote.txid).put("account", quote.accountFingerprint).toString())
        db.orderDao().update(order.copy(paymentStatus = "PENDENTE", syncStatus = if (br.com.ruy.pos.BuildConfig.HOMOLOGATION) "HOMOLOGATION" else "PENDING"))
        a.copy(stoneOrderId = db.stoneDao().insert(a))
    }

    data class ManualReceiptProof(val reference: Long, val orderId: String, val amountCents: Int, val received: Boolean,
        val operator: String, val evidence: String, val receipt: String, val checked: Boolean)

    suspend fun confirmPix(input: ManualReceiptProof) = db.withTransaction {
        br.com.ruy.pos.security.PaymentAudit.validate(input.operator, input.evidence, input.receipt)
        require(input.checked && input.operator.trim().length in 3..80 && input.evidence.trim().length in 8..500) { "Confira o recebimento no banco e informe operador e comprovante" }
        val a = requireNotNull(db.stoneDao().byReference(input.reference))
        check(a.origin == "PIX_STATIC")
        val p = requireNotNull(db.paymentDao().byId(a.paymentId))
        require(p.orderId == input.orderId && p.amountCents == input.amountCents) { "Pedido ou valor divergente" }
        val receipt = input.receipt.trim()
        if (a.activeSlot == null) {
            check((a.state == "CONFIRMADO") == input.received && (!input.received || p.transactionRef.equals(receipt, ignoreCase = true))) { "Confirmação conflitante" }
            return@withTransaction
        }
        if (input.received) {
            require(receipt.matches(Regex("[A-Za-z0-9._-]{3,128}"))) { "Informe a referência do comprovante bancário" }
            check(db.paymentDao().getAll().none { it.paymentId != p.paymentId && it.status == "PAGO" && it.transactionRef.equals(receipt, ignoreCase = true) }) { "Comprovante já utilizado" }
            settle(p.copy(transactionRef = receipt))
        } else db.paymentDao().update(p.copy(status = "SEM_RECEBIMENTO"))
        db.stoneDao().update(a.copy(state = if (input.received) "CONFIRMADO" else "SEM_RECEBIMENTO", activeSlot = null,
            reconciledBy = input.operator.trim(), reconciliationEvidence = "MANUAL_PIX: ${input.evidence.trim()}", updatedAt = System.currentTimeMillis()))
    }

    suspend fun confirmSimulated(orderId: String, amountCents: Int): String = db.withTransaction {
        check(br.com.ruy.pos.BuildConfig.HOMOLOGATION) { "Simulação exclusiva da homologação" }
        requireNoFlight()
        val session = requireNotNull(db.cashSessionDao().getOpen())
        val p = PaymentEntity(UUID.randomUUID().toString(), orderId, amountCents, "SIMULADO", "CRIADA", cashSessionId = session.sessionId)
        db.paymentDao().insert(p)
        settle(p)
        p.paymentId
    }

    private suspend fun verifyComanda(order: OrderEntity) {
        order.comandaId?.let { id ->
            val comanda = requireNotNull(db.tableOrderDao().byId(id))
            check(comanda.status == "ABERTA") { "Comanda encerrada" }
            val total = db.tableOrderItemDao().getItems(id).sumOf { it.unitCents.toLong() * it.qty }
            check(total == order.totalCents.toLong()) { "Comanda alterada; atualize o checkout antes de cobrar" }
        }
    }

    private suspend fun settle(payment: PaymentEntity) {
        val order = requireNotNull(db.orderDao().byId(payment.orderId))
        val existing = requireNotNull(db.paymentDao().byId(payment.paymentId))
        if (existing.status == "PAGO") return
        verifyComanda(order)
        require(payment.amountCents in 1..remaining(order)) { "Recebimento excede saldo; conferir pagamentos do pedido" }
        val session = requireNotNull(db.cashSessionDao().getOpen()) { "Caixa da tentativa deve estar aberto" }
        check(session.sessionId == payment.cashSessionId) { "Sessão de Caixa divergente" }
        db.paymentDao().update(payment.copy(status = "PAGO"))
        db.cashMovementDao().insert(CashMovementEntity("payment:${payment.paymentId}", session.sessionId,
            "VENDA_${payment.method}", payment.amountCents, "Recebimento confirmado", order.id))
        val fullyPaid = remaining(order) == 0
        db.orderDao().update(order.copy(paymentStatus = if (fullyPaid) "PAGO" else "PARCIAL", syncStatus = if (br.com.ruy.pos.BuildConfig.HOMOLOGATION) "HOMOLOGATION" else "PENDING",
            stoneTransactionId = payment.transactionRef ?: order.stoneTransactionId))
        if (fullyPaid) order.comandaId?.let { id ->
            val comanda = requireNotNull(db.tableOrderDao().byId(id))
            db.tableOrderDao().updateStatus(id, "FECHADA", System.currentTimeMillis())
            check(db.tableOrderDao().getOpenByTable(comanda.tableId) == null) { "Mesa tem outra comanda aberta" }
            db.tableDao().updateStatus(comanda.tableId, "LIVRE", null)
        }
    }

    suspend fun closeCash(sessionId: String, countedCents: Int): CashClosingEntity = db.withTransaction {
        require(countedCents >= 0)
        val session = requireNotNull(db.cashSessionDao().getOpen()) { "Caixa fechado" }
        check(session.sessionId == sessionId)
        requireNoFlight()
        fun checkedCents(value: Long): Int {
            require(value in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) { "Totais do Caixa excedem o limite de centavos" }
            return value.toInt()
        }
        val moves = db.cashMovementDao().getBySession(sessionId)
        fun total(type: String) = checkedCents(moves.filter { it.type == type }.sumOf { it.amountCents.toLong() })
        val sales = checkedCents(moves.filter { it.type.startsWith("VENDA_") }.sumOf { it.amountCents.toLong() })
        val cash = total("VENDA_DINHEIRO")
        val expected = checkedCents(session.openingBalanceCents.toLong() + cash + total("SUPRIMENTO") - total("SANGRIA"))
        val closing = CashClosingEntity(UUID.randomUUID().toString(), sessionId, System.currentTimeMillis(), expected,
            countedCents, checkedCents(countedCents.toLong() - expected), sales, cash,
            checkedCents(total("VENDA_PIX").toLong() + total("VENDA_INSTANT_PAYMENT") + total("VENDA_PIX_STATIC") + total("VENDA_PIX_API")),
            checkedCents(total("VENDA_DEBIT").toLong() + total("VENDA_TAPSTONE_DEBIT")),
            checkedCents(total("VENDA_CREDIT").toLong() + total("VENDA_TAPSTONE_CREDIT")), total("SANGRIA"), total("SUPRIMENTO"), session.operator,
            JSONObject().put("environment", if (br.com.ruy.pos.BuildConfig.HOMOLOGATION) "HOMOLOGATION" else "COMMERCIAL")
                .put("receivedByMode", JSONObject(br.com.ruy.pos.report.PaymentBreakdowns.calculate(db.paymentDao().getAll().filter { it.cashSessionId == sessionId }).received))
                .put("pendingCents", 0).toString())
        db.cashClosingDao().insert(closing)
        db.cashSessionDao().close(sessionId, closing.closedAt)
        closing
    }
}
