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

class Repository(context: Context) {
    private val db = AppDatabase.get(context)
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
        categoryDao.insert(CategoryEntity(id = id, name = name, active = true))
    }
    suspend fun saveProductLocal(id: String, categoryId: String?, name: String, priceCents: Int, active: Boolean = true, imagePath: String? = null) {
        productLocalDao.insert(ProductLocalEntity(id = id, categoryId = categoryId, name = name, priceCents = priceCents, active = active, imagePath = imagePath))
    }

    suspend fun saveOrder(totalCents: Int, items: List<Pair<String,Int>>, unitCents: Map<String,Int>, deviceId: String?, operatorId: String?): String {
        val id = UUID.randomUUID().toString()
        val arr = JSONArray()
        items.forEach { (name, qty) ->
            arr.put(JSONObject().put("name", name).put("qty", qty).put("unitCents", unitCents[name] ?: 0))
        }
        val order = OrderEntity(
            id = id,
            totalCents = totalCents,
            paymentStatus = "NAO_PAGO",
            syncStatus = "PENDING",
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
        val pid = UUID.randomUUID().toString()
        paymentDao.insert(PaymentEntity(paymentId = pid, orderId = orderId, amountCents = amountCents, method = method, status = status, receivedCents = receivedCents, changeCents = changeCents, installments = installments, transactionRef = transactionRef))
        return pid
    }

    suspend fun getPayments(orderId: String) = paymentDao.getByOrder(orderId)

    suspend fun closeComandaIfPaid(comandaId: String, totalCents: Int): Boolean {
        val items = tableOrderItemDao.getItems(comandaId)
        val total = if (items.isEmpty()) totalCents else items.sumOf { it.unitCents * it.qty }
        // For now, mark as closed only if we have a confirmed payment flow - caller decides
        return false
    }

    suspend fun markTableFree(tableId: String) {
        tableDao.updateStatus(tableId, "LIVRE", null)
    }

    suspend fun closeComanda(comandaId: String) {
        val items = tableOrderItemDao.getItems(comandaId)
        val total = items.sumOf { it.unitCents * it.qty }
        tableOrderDao.updateTotal(comandaId, total)
    }

    suspend fun ensureTables(count: Int) {
        for (i in 1..count) {
            val id = "T%02d".format(i)
            tableDao.upsertTable(TableEntity(tableId = id, number = i, status = "LIVRE"))
        }
    }

    suspend fun getTables() = tableDao.getAll()

    suspend fun openTable(tableId: String): String {
        val existing = tableOrderDao.getOpenByTable(tableId)
        if (existing != null) return existing.comandaId
        val comandaId = UUID.randomUUID().toString()
        tableDao.updateStatus(tableId, "OCUPADA", System.currentTimeMillis())
        tableOrderDao.insert(TableOrderEntity(comandaId = comandaId, tableId = tableId, status = "ABERTA"))
        return comandaId
    }

    suspend fun getOpenComanda(tableId: String) = tableOrderDao.getOpenByTable(tableId)

    suspend fun addItemToComanda(comandaId: String, productName: String, unitCents: Int, qty: Int, productId: String? = null) {
        val itemId = UUID.randomUUID().toString()
        tableOrderItemDao.insert(TableOrderItemEntity(itemId = itemId, comandaId = comandaId, productId = productId, productName = productName, unitCents = unitCents, qty = qty))
        val items = tableOrderItemDao.getItems(comandaId)
        val total = items.sumOf { it.unitCents * it.qty }
        tableOrderDao.updateTotal(comandaId, total)
    }

    suspend fun getComandaItems(comandaId: String) = tableOrderItemDao.getItems(comandaId)
    suspend fun getComandaTotal(comandaId: String): Int {
        val items = tableOrderItemDao.getItems(comandaId)
        return items.sumOf { it.unitCents * it.qty }
    }
}

