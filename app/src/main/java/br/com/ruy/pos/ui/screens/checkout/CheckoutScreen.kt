package br.com.ruy.pos.ui.screens.checkout

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.ruy.pos.data.Repository
import br.com.ruy.pos.payment.*
import br.com.ruy.pos.ui.components.*
import br.com.ruy.pos.ui.theme.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import org.json.JSONArray

@Composable
private fun CheckLine(text: String, checked: Boolean, onCheck: (Boolean) -> Unit) {
    Row { Checkbox(checked = checked, onCheckedChange = onCheck); Text(text, modifier = Modifier.padding(top = 12.dp)) }
}

@Composable
fun ExternalPaymentDialog(orderId: String, method: String, amountCents: Int, busy: Boolean,
                           error: String?, onDismiss: () -> Unit, onConfirm: (ExternalPaymentInput) -> Unit) {
    var operator by remember { mutableStateOf("") }
    var evidence by remember { mutableStateOf("") }
    var reference by remember { mutableStateOf("") }
    var installments by remember { mutableStateOf("1") }

    var checked by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = { if (!busy) onDismiss() }, title = { Text("Pagamento externo confirmado") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Pedido ${orderId.take(8).uppercase()} • ${Money.format(amountCents.toLong())} • ${OperationalText.method(method)}")
            Text("O pagamento foi efetuado em outro provedor/maquininha. Nenhum dado do cartão é coletado pelo Ruy POS.")
            OutlinedTextField(reference, { reference = it }, label = { Text("Identificador do comprovante (opcional)") })
            if (method == "CREDITO") OutlinedTextField(installments, { installments = it }, label = { Text("Parcelas (1 a 99)") })
            OutlinedTextField(operator, { operator = it }, label = { Text("Operador responsável") })
            OutlinedTextField(evidence, { evidence = it }, label = { Text("Comprovante/extrato consultado (data/hora e identificador)") })
            CheckLine("Conferi no comprovante/extrato do provedor que este valor foi recebido para este pedido", checked) { checked = it }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }, confirmButton = { TextButton(enabled = !busy && checked && operator.trim().length >= 3 && evidence.trim().length >= 8,
        onClick = { onConfirm(ExternalPaymentInput(operator.trim(), evidence.trim(), reference.ifBlank { null }, installments.toIntOrNull() ?: 1)) }) {
        Text(if (busy) "Salvando…" else "Registrar recebimento") } },
        dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text("Voltar") } })
}

data class ExternalPaymentInput(val operator: String, val evidence: String, val reference: String?, val installments: Int)

@Composable
fun CheckoutScreen(
    repo: Repository,
    totalCents: Int,
    items: List<Pair<String, Int>>,
    unitPrice: Map<String, Int>,
    comandaId: String? = null,
    tableId: String? = null,
    existingOrderId: String? = null,
    onBack: () -> Unit,
    onPaidAndClosed: () -> Unit = onBack
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    fun reais(c: Int) = NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(c / 100.0)
    fun notify(message: String) { Toast.makeText(context, message, Toast.LENGTH_LONG).show() }
    var orderId by rememberSaveable { mutableStateOf(existingOrderId) }
    var method by remember { mutableStateOf("DINHEIRO") }
    var received by remember { mutableStateOf("") }


    var amount by remember { mutableStateOf("") }
    var processing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var setup by remember { mutableStateOf(false) }
    var reconciliation by remember { mutableStateOf(false) }
    var external by remember { mutableStateOf(false) }
    var tapProof by remember { mutableStateOf(false) }
    var pixDialog by remember { mutableStateOf(false) }
    var pixQuote by remember { mutableStateOf<br.com.ruy.pos.payment.pix.PixQuote?>(null) }
    var tapCredit by remember { mutableStateOf(false) }
    var tapInstallments by remember { mutableStateOf("1") }
    var tapTermsChecked by remember { mutableStateOf(false) }
    var stoneAccessChecked by remember { mutableStateOf(false) }
    var readiness by remember { mutableStateOf(TapStoneReadiness.read(context)) }
    val lifecycle = androidx.compose.ui.platform.LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) readiness = TapStoneReadiness.read(context)
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    var configuration by remember { mutableStateOf(StoneConfiguration.read(context)) }
    val stone = remember(configuration) { StonePaymentGateway(context, configuration) }
    val active by remember { repo.payments.observeActive() }.collectAsState(initial = null)

    LaunchedEffect(Unit) {
        try { if (orderId == null) orderId = repo.prepareCheckout(totalCents, items, unitPrice, comandaId) }
        catch (e: Exception) { error = br.com.ruy.pos.ui.components.OperationalText.error(e) }
    }
    val order by remember(orderId) { repo.payments.observeOrder(orderId.orEmpty()) }.collectAsState(initial = null)
    val payments by remember(orderId) { repo.payments.observePayments(orderId.orEmpty()) }.collectAsState(initial = emptyList())
    val attempts by remember(orderId) { repo.payments.observeAttempts(orderId.orEmpty()) }.collectAsState(initial = emptyList())
    val paid = payments.filter { it.status == "PAGO" }.sumOf { it.amountCents }
    val remaining = ((order?.totalCents ?: totalCents) - paid).coerceAtLeast(0)
    val amountCents = Money.cents(amount) ?: 0
    val receivedCents = Money.cents(received) ?: 0
    val change = if (method == "DINHEIRO" && receivedCents >= amountCents) receivedCents - amountCents else 0
    val attempt = attempts.firstOrNull { it.activeSlot == 1 }
    val payment = payments.firstOrNull { it.paymentId == attempt?.paymentId }
    val legacy = payments.firstOrNull { p -> p.status in setOf("AGUARDANDO", "INDETERMINADO", "EM_CONCILIACAO", "CRIADA") && attempts.none { it.paymentId == p.paymentId } }
    val summary = remember(order?.itemsJson, items, unitPrice) {
        order?.let { saved ->
            val arr = JSONArray(saved.itemsJson)
            (0 until arr.length()).map { arr.getJSONObject(it) }.map { Triple(it.getString("name"), it.getInt("qty"), it.getInt("unitCents")) }
        } ?: items.map { (name, qty) -> Triple(name, qty, unitPrice[name] ?: 0) }
    }
    LaunchedEffect(remaining) { amount = Money.input(remaining) }
    LaunchedEffect(attempt?.attemptId) {
        if (attempt?.origin == "PIX_STATIC") {
            pixQuote = try { kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { br.com.ruy.pos.payment.pix.PixInvoiceStore(context).load(attempt.attemptId) } }
            catch (e: Exception) { SupportDiagnostics.record(e); null }
        }
    }
    LaunchedEffect(order?.paymentStatus, active) {
        if (order?.paymentStatus == "PAGO" && active == null) onPaidAndClosed()
    }

    if (pixDialog && attempt?.origin == "PIX_STATIC" && payment != null) PixPaymentDialog(attempt, payment, pixQuote, processing, error,
        onDismiss = { pixDialog = false; error = null }, onConfirm = { proof ->
            if (!processing) { processing = true; scope.launch {
                try { repo.payments.confirmPix(proof); pixDialog = false; error = null }
                catch (e: Exception) { error = OperationalText.error(e) }
                finally { processing = false }
            } }
        })
    if (external) ExternalPaymentDialog(orderId ?: "", method, amountCents, processing, error,
        onDismiss = { external = false; error = null }, onConfirm = { input ->
            if (!processing) {
                processing = true
                scope.launch {
                    try {
                        repo.payments.confirmExternal(orderId ?: "", amountCents, method, input.operator, input.evidence, input.installments, input.reference)
                        external = false
                        error = null
                        notify("Pagamento externo registrado como PAGO_MANUALMENTE no Caixa")
                    } catch (e: Exception) { error = br.com.ruy.pos.ui.components.OperationalText.error(e) }
                    finally { processing = false }
                }
            }
        })
    if (setup) StoneSetupDialog(configuration, onDismiss = { setup = false }, onSaved = { configuration = it; setup = false })
    if (tapProof && attempt?.origin == "TAPSTONE_EXTERNAL" && payment != null) TapStoneDialog(attempt, payment, processing, error,
        onDismiss = { tapProof = false; error = null }, onConfirm = { proof ->
            if (!processing) {
                processing = true
                scope.launch {
                    try { repo.payments.confirmTapStone(proof); tapProof = false; error = null; notify(if (proof.received) "Pagamento confirmado manualmente — TapStone externo." else "Conferência salva sem recebimento") }
                    catch (e: Exception) { error = br.com.ruy.pos.ui.components.OperationalText.error(e) }
                    finally { processing = false }
                }
            }
        })
    if (reconciliation && attempt != null && payment != null && attempt.origin !in setOf("TAPSTONE_EXTERNAL", "PIX_STATIC")) StoneReconciliationDialog(attempt, payment, processing,
        error = error, onDismiss = { reconciliation = false; error = null }, onConfirm = { input ->
            if (!processing) {
                processing = true
                scope.launch {
                    try {
                        repo.payments.reconcile(input)
                        reconciliation = false
                        error = null
                        notify(if (input.received) "Recebimento conciliado e registrado no Caixa" else "Ausência de recebimento conferida; tentativa encerrada")
                    } catch (e: Exception) { error = br.com.ruy.pos.ui.components.OperationalText.error(e) }
                    finally { processing = false }
                }
            }
        })

    Column(Modifier.fillMaxSize().background(RuyBackground)) {
        RuyHeader(title = "Finalizar venda", subtitle = if (tableId != null) "Mesa ${tableId.takeLast(2)}" else "Balcão")
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            RuyCard {
                Text("Resumo do pedido", style = MaterialTheme.typography.titleSmall)
                summary.forEach { (name, qty, price) ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("$name x$qty", style = MaterialTheme.typography.bodyMedium)
                        Text(reais(price * qty), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 8.dp), color = RuyBorder)
                Text("Total: ${reais(order?.totalCents ?: totalCents)} • Recebido: ${reais(paid)}")
                Text("Saldo a pagar: ${reais(remaining)}", color = RuyRed, fontWeight = FontWeight.SemiBold)
                orderId?.let { Text("Pedido ${it.take(8)}", style = MaterialTheme.typography.bodySmall) }
            }
            RuyCard {
                Text("Forma de pagamento", style = MaterialTheme.typography.titleSmall)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("DINHEIRO" to "Dinheiro", "PIX" to "Pix QR Code", "DEBITO" to "Débito externo", "CREDITO" to "Crédito externo").forEach { (value, label) ->
                        FilterChip(selected = method == value, enabled = !processing && active == null,
                            onClick = { method = value }, label = { Text(label) })
                    }
                }
                OutlinedTextField(amount, { amount = it }, enabled = !processing && active == null,
                    label = { Text("Valor desta cobrança (R$)") }, modifier = Modifier.fillMaxWidth())
                if (method == "DINHEIRO") {
                    OutlinedTextField(received, { received = it }, enabled = !processing && active == null,
                        label = { Text("Valor recebido (R$)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    Text("Troco: ${reais(change)}")
                }
                if (method == "PIX") {
                    Text("QR estático • confirme o crédito no banco. Configure a conta em Mais > Configurações > Pagamentos.", style = MaterialTheme.typography.bodySmall)
                } else if (method != "DINHEIRO") {
                    Text("Realize a cobrança no equipamento autorizado e confira a aprovação antes de registrar.", style = MaterialTheme.typography.bodySmall)
                    TextButton(enabled = !br.com.ruy.pos.BuildConfig.HOMOLOGATION && !processing && active == null, onClick = { external = true }) { Text("Pagamento externo já ocorreu (maquininha/banco)") }
                }
            }
            RuyCard {
                Text("Aproximação no celular", style = MaterialTheme.typography.titleSmall)
                Text(readiness.operatorDescription, style = MaterialTheme.typography.bodySmall)
                Text("TapStone processado no aplicativo oficial Stone. Confirmação manual; sem integração automática.")
                if (br.com.ruy.pos.BuildConfig.HOMOLOGATION) {
                    Text("Indisponível para cobrança na homologação. Use pagamento simulado ou dinheiro fictício.")
                    TextButton(enabled = !processing && active == null && remaining > 0 && order != null, onClick = {
                        processing = true
                        scope.launch {
                            try { repo.payments.confirmSimulated(requireNotNull(orderId), amountCents) }
                            catch (e: Exception) { error = br.com.ruy.pos.ui.components.OperationalText.error(e) }
                            finally { processing = false }
                        }
                    }) { Text("Pagamento simulado — dados fictícios") }
                } else {
                    CheckLine("Conferi: aplicativo oficial Stone instalado e TapStone autorizado na conta", stoneAccessChecked) { stoneAccessChecked = it }
                    Row {
                        FilterChip(!tapCredit, { tapCredit = false }, label = { Text("Débito à vista") }, enabled = !processing)
                        FilterChip(tapCredit, { tapCredit = true }, label = { Text("Crédito") }, enabled = !processing)
                    }
                    if (tapCredit) {
                        OutlinedTextField(tapInstallments, { tapInstallments = it; tapTermsChecked = false }, label = { Text("Parcelas aprovadas pela Stone • 1 a 18") })
                        CheckLine("Conferi as condições de parcelamento disponíveis no provedor; sem acréscimos no Ruy POS", tapTermsChecked) { tapTermsChecked = it }
                    }
                    TextButton(onClick = { readiness = TapStoneReadiness.read(context) }) { Text("Atualizar compatibilidade") }
                    TextButton(enabled = !processing && active == null && readiness.locallyReady && stoneAccessChecked && order != null && remaining > 0,
                        onClick = {
                            processing = true
                            scope.launch {
                                try {
                                    readiness = TapStoneReadiness.read(context)
                                    check(readiness.locallyReady) { "Android 10+, NFC ativo e internet necessários" }
                                    check(!tapCredit || tapTermsChecked) { "Confira as condições no provedor" }
                                    repo.payments.beginTapStone(requireNotNull(orderId), amountCents, if (tapCredit) "TAPSTONE_CREDIT" else "TAPSTONE_DEBIT", if (tapCredit) tapInstallments.toIntOrNull() ?: 0 else 1)
                                    tapProof = true
                                } catch (e: Exception) { error = br.com.ruy.pos.ui.components.OperationalText.error(e) }
                                finally { processing = false }
                            }
                        }) { Text("Preparar conferência Stone externa") }
                    Text("Abra a Stone manualmente. Disponibilidade da conta e do aplicativo confirmada pelo operador.")
                }
            }
            if (attempt != null) RuyCard {
                Text(OperationalText.status(attempt.state), style = MaterialTheme.typography.titleSmall)
                Text("Confira o comprovante no banco ou provedor antes de registrar o recebimento. Voltar ao pedido não confirma pagamento.")
                TextButton(enabled = !processing, onClick = { error = null; when(attempt.origin) { "PIX_STATIC" -> pixDialog = true; "TAPSTONE_EXTERNAL" -> tapProof = true; else -> reconciliation = true } }) { Text("Conferir recebimento") }
            }
            if (active != null && attempt == null) Text("Existe pagamento pendente em outro pedido. Retome-o antes de cobrar novamente.")
            if (legacy != null && active == null) TextButton(enabled = !processing, onClick = {
                processing = true
                scope.launch {
                    try { repo.payments.recoverLegacyPayment(legacy.paymentId) }
                    catch (e: Exception) { error = br.com.ruy.pos.ui.components.OperationalText.error(e) }
                    finally { processing = false }
                }
            }) { Text("Recuperar pagamento anterior para conciliação") }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            RuyPrimaryButton(text = if (processing) "Processando…" else if (method == "PIX") "Gerar QR Pix" else if (method == "DINHEIRO") "Confirmar dinheiro" else "Conferir pagamento externo",
                enabled = !processing && order != null && active == null && remaining > 0,
                onClick = {
                    if (processing) return@RuyPrimaryButton
                    processing = true
                    error = null
                    scope.launch {
                        try {
                            val id = requireNotNull(orderId) { "Pedido ainda não salvo" }
                            require(amountCents in 1..remaining) { "Valor deve ser positivo e não exceder o saldo" }
                            if (method == "DINHEIRO") {
                                repo.payments.confirmCash(id, amountCents, receivedCents)
                                notify("Dinheiro registrado. Troco: ${reais(change)}")
                            } else if (method == "PIX") {
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                    val service = br.com.ruy.pos.payment.pix.PixCheckoutService(repo.payments,
                                        br.com.ruy.pos.payment.pix.PixSettingsStore(context), br.com.ruy.pos.payment.pix.PixInvoiceStore(context))
                                    service.prepare(id, amountCents)
                                }
                                pixDialog = true
                            } else {
                                check(!br.com.ruy.pos.BuildConfig.HOMOLOGATION) { "Use pagamento simulado em homologação" }
                                external = true
                                return@launch

                            }
                        } catch (e: Exception) { error = br.com.ruy.pos.ui.components.OperationalText.error(e) }
                        finally { processing = false }
                    }
                })
            OutlinedButton(onClick = onBack, enabled = !processing, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(12.dp)) { Text("Voltar") }
        }
    }
}
