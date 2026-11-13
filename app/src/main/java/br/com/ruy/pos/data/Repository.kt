package br.com.ruy.pos.data

import android.content.Context
import br.com.ruy.pos.data.local.AppDatabase
import br.com.ruy.pos.data.local.OrderEntity
import br.com.ruy.pos.data.local.ProductEntity
import br.com.ruy.pos.data.local.CategoryEntity
import br.com.ruy.pos.data.local.ProductLocalEntity
import br.com.ruy.pos.data.local.TableEntity
import br.com.ruy.pos.data.local.TableOrderEntity
import br.com.ruy.pos.data.local.TableOrderItemEntity
import br.com.ruy.pos.data.local.PaymentEntity
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import androidx.room.withTransaction
import br.com.ruy.pos.payment.PaymentCoordinator

class Repository internal constructor(private val db: AppDatabase, options: () -> br.com.ruy.pos.payment.ExternalPaymentOptions = { br.com.ruy.pos.payment.ExternalPaymentOptions() }) {
    constructor(context: Context) : this(AppDatabase.get(context), { br.com.ruy.pos.payment.PaymentCapabilitiesStore(context).load() })
    val payments = PaymentCoordinator(db, options)
    fun observeOrders() = orderDao.observeAll()
    fun observePayments() = paymentDao.observeAll()
    fun observeTables() = tableDao.observeAll()
    fun observeCashMovements(sessionId: String) = db.cashMovementDao().observe(sessionId)
    suspend fun getOrder(id: String) = orderDao.byId(id)
    suspend fun getComanda(id: String) = tableOrderDao.byId(id)

    suspend fun prepareCheckout(totalCents: Int, items: List<Pair<String, Int>>, unitCents: Map<String, Int>,
                                comandaId: String? = null, existingOrderId: String? = null): String = db.withTransaction {
        existingOrderId?.let { return@withTransaction requireNotNull(orderDao.byId(it)).id }
        comandaId?.let { id ->
            val comanda = requireNotNull(tableOrderDao.byId(id)) { "Comanda não encontrada" }
            check(comanda.status == "ABERTA") { "Comanda já encerrada" }
            orderDao.byComanda(id)?.let { return@withTransaction it.id }
            val savedItems = tableOrderItemDao.getItems(id)
            val total = savedItems.sumOf { it.unitCents.toLong() * it.qty }
            require(total in 1..Int.MAX_VALUE.toLong() && total == totalCents.toLong()) { "Total da comanda divergente" }
            val orderId = saveOrder(totalCents, savedItems.map { it.productName to it.qty },
                savedItems.associate { it.productName to it.unitCents }, null, null)
            val order = requireNotNull(orderDao.byId(orderId))
            // Preserve each historical price rather than the current product catalog price.
            val arr = JSONArray()
            savedItems.forEach { arr.put(JSONObject().put("name", it.productName).put("qty", it.qty).put("unitCents", it.unitCents)) }
            orderDao.update(order.copy(comandaId = id, itemsJson = arr.toString()))
            return@withTransaction orderId
        }
        require(totalCents > 0 && items.sumOf { (name, qty) -> (unitCents[name] ?: 0).toLong() * qty } == totalCents.toLong()) {
            "Itens e total do pedido divergem"
        }
        saveOrder(totalCents, items, unitCents, null, null)
    }
    private val productDao = db.productDao()
    private val orderDao = db.orderDao()
    private val categoryDao = db.categoryDao()
    private val productLocalDao = db.productLocalDao()
    private val tableDao = db.tableDao()
    private val tableOrderDao = db.tableOrderDao()
    private val tableOrderItemDao = db.tableOrderItemDao()
    private val paymentDao = db.paymentDao()

    suspend fun products() = productDao.getActive()
    suspend fun categories() = categoryDao.getActive()
    suspend fun productLocal() = productLocalDao.getActive()
    suspend fun productLocalAll() = productLocalDao.getAll()
    
    suspend fun saveCategory(id: String, name: String) {
        require(id.isNotBlank() && name.trim().length in 1..80) { "Informe o nome da categoria" }
        categoryDao.insert(CategoryEntity(id = id, name = name.trim(), active = true))
    }
    suspend fun saveProductLocal(id: String, categoryId: String?, name: String, priceCents: Int, active: Boolean = true, imagePath: String? = null) {
        require(id.isNotBlank() && name.trim().length in 1..120 && priceCents > 0) { "Informe nome e preço válidos" }
        productLocalDao.insert(ProductLocalEntity(id = id, categoryId = categoryId, name = name.trim(), priceCents = priceCents, active = active, imagePath = imagePath))
    }

    suspend fun saveOrder(totalCents: Int, items: List<Pair<String,Int>>, unitCents: Map<String,Int>, deviceId: String?, operatorId: String?): String {
        require(totalCents > 0 && items.isNotEmpty() && items.all { (name, qty) -> name.isNotBlank() && qty > 0 && (unitCents[name] ?: 0) > 0 } &&
            items.sumOf { (name, qty) -> requireNotNull(unitCents[name]).toLong() * qty } == totalCents.toLong()) { "Confira itens e valor do pedido" }
        val id = UUID.randomUUID().toString()
        val arr = JSONArray()
        items.forEach { (name, qty) ->
            arr.put(JSONObject().put("name", name).put("qty", qty).put("unitCents", unitCents[name] ?: 0))
        }
        val order = OrderEntity(
            id = id,
            totalCents = totalCents,
            paymentStatus = "NAO_PAGO",
            syncStatus = if (br.com.ruy.pos.BuildConfig.HOMOLOGATION) "HOMOLOGATION" else "PENDING",
            itemsJson = arr.toString(),
            createdAt = System.currentTimeMillis(),
            operatorId = operatorId,
            deviceId = deviceId,
            stoneTransactionId = null
        )
        orderDao.insert(order)
        return id
    }

    suspend fun pendingOrders() = orderDao.pending()
    suspend fun markSynced(id: String) = orderDao.updateSyncStatus(id, "SYNCED")
    suspend fun allOrders() = orderDao.all()

    suspend fun savePayment(orderId: String, amountCents: Int, method: String, status: String, receivedCents: Int? = null, changeCents: Int? = null, installments: Int = 1, transactionRef: String? = null): String {
        require(status != "PAGO") { "Confirme recebimentos pelo fluxo transacional de Caixa/conciliação" }
        val pid = UUID.randomUUID().toString()
        paymentDao.insert(PaymentEntity(paymentId = pid, orderId = orderId, amountCents = amountCents, method = method, status = status, receivedCents = receivedCents, changeCents = changeCents, installments = installments, transactionRef = transactionRef))
        return pid
    }

    suspend fun getPayments(orderId: String) = paymentDao.getByOrder(orderId)
    suspend fun getAllPayments() = paymentDao.getAll()

    suspend fun getOpenCashSession() = db.cashSessionDao().getOpen()
    suspend fun openCashSession(operator: String, openingBalanceCents: Int): String = db.withTransaction {
        require(operator.isNotBlank() && openingBalanceCents >= 0)
        check(db.cashSessionDao().getOpen() == null) { "Caixa já aberto" }
        val id = UUID.randomUUID().toString()
        db.cashSessionDao().insert(br.com.ruy.pos.data.local.CashSessionEntity(sessionId = id, operator = operator, openedAt = System.currentTimeMillis(), closedAt = null, openingBalanceCents = openingBalanceCents, status = "ABERTO"))
        id
    }
    suspend fun addCashMovement(sessionId: String, type: String, amountCents: Int, reason: String?, orderId: String? = null) = db.withTransaction {
        require(type in setOf("SANGRIA", "SUPRIMENTO") && amountCents > 0 && !reason.isNullOrBlank()) { "Movimento de caixa inválido" }
        check(db.cashSessionDao().getOpen()?.sessionId == sessionId) { "Caixa fechado ou divergente" }
        val mid = UUID.randomUUID().toString()
        db.cashMovementDao().insert(br.com.ruy.pos.data.local.CashMovementEntity(movementId = mid, sessionId = sessionId, type = type, amountCents = amountCents, reason = reason, orderId = orderId, createdAt = System.currentTimeMillis()))
    }
    suspend fun getCashMovements(sessionId: String) = db.cashMovementDao().getBySession(sessionId)
    suspend fun getCashClosings() = db.cashClosingDao().getAll()

    suspend fun closeComandaIfPaid(comandaId: String): Boolean {
        return tableOrderDao.byId(comandaId)?.status == "FECHADA" && orderDao.byComanda(comandaId)?.paymentStatus == "PAGO"
    }

    suspend fun markTableFree(tableId: String) {
        check(tableOrderDao.getOpenByTable(tableId) == null) { "Mesa tem comanda aberta; conclua a quitação" }
        tableDao.updateStatus(tableId, "LIVRE", null)
    }

    suspend fun closeComanda(comandaId: String) {
        check(closeComandaIfPaid(comandaId)) { "Conclua a quitação pelo checkout" }
    }

    suspend fun ensureTables(count: Int) {
        for (i in 1..count) {
            val id = "T%02d".format(i)
            tableDao.upsertTable(TableEntity(tableId = id, number = i, status = "LIVRE"))
        }
    }

    suspend fun getTables() = tableDao.getAll()

    suspend fun openTable(tableId: String): String = db.withTransaction {
        val existing = tableOrderDao.getOpenByTable(tableId)
        if (existing != null) return@withTransaction existing.comandaId
        val comandaId = UUID.randomUUID().toString()
        tableDao.updateStatus(tableId, "OCUPADA", System.currentTimeMillis())
        tableOrderDao.insert(TableOrderEntity(comandaId = comandaId, tableId = tableId, status = "ABERTA"))
        comandaId
    }

    suspend fun getOpenComanda(tableId: String) = tableOrderDao.getOpenByTable(tableId)

    suspend fun addItemToComanda(comandaId: String, productName: String, unitCents: Int, qty: Int, productId: String? = null) = db.withTransaction {
        require(unitCents > 0 && qty > 0)
        check(tableOrderDao.byId(comandaId)?.status == "ABERTA") { "Comanda encerrada" }
        val linkedOrder = orderDao.byComanda(comandaId)
        check(linkedOrder == null || (linkedOrder.paymentStatus != "PAGO" && db.stoneDao().active()?.orderId != linkedOrder.id &&
            paymentDao.getByOrder(linkedOrder.id).none { it.status in setOf("CRIADA", "AGUARDANDO", "INDETERMINADO", "EM_CONCILIACAO") })) {
            "Comanda com pagamento pendente; concilie antes de alterar itens"
        }
        val itemId = UUID.randomUUID().toString()
        tableOrderItemDao.insert(TableOrderItemEntity(itemId = itemId, comandaId = comandaId, productId = productId, productName = productName, unitCents = unitCents, qty = qty))
        val items = tableOrderItemDao.getItems(comandaId)
        val totalLong = items.sumOf { it.unitCents.toLong() * it.qty }
        require(totalLong in 1..Int.MAX_VALUE.toLong()) { "Total da comanda excede limite" }
        val total = totalLong.toInt()
        tableOrderDao.updateTotal(comandaId, total)
        linkedOrder?.let { order ->
            val arr = JSONArray()
            items.forEach { arr.put(JSONObject().put("name", it.productName).put("qty", it.qty).put("unitCents", it.unitCents)) }
            val paid = paymentDao.getByOrder(order.id).any { it.status == "PAGO" }
            orderDao.update(order.copy(totalCents = total, itemsJson = arr.toString(), paymentStatus = if (paid) "PARCIAL" else "NAO_PAGO", syncStatus = if (br.com.ruy.pos.BuildConfig.HOMOLOGATION) "HOMOLOGATION" else "PENDING"))
        }
    }

    suspend fun getComandaItems(comandaId: String) = tableOrderItemDao.getItems(comandaId)
    suspend fun getComandaTotal(comandaId: String): Int {
        val items = tableOrderItemDao.getItems(comandaId)
        return items.sumOf { it.unitCents * it.qty }
    }
}

