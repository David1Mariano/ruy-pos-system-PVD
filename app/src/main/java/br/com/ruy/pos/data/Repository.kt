package br.com.ruy.pos.data

import android.content.Context
import br.com.ruy.pos.data.local.AppDatabase
import br.com.ruy.pos.data.local.OrderEntity
import br.com.ruy.pos.data.local.ProductEntity
import br.com.ruy.pos.data.local.CategoryEntity
import br.com.ruy.pos.data.local.ProductLocalEntity
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class Repository(context: Context) {
    private val db = AppDatabase.get(context)
    private val productDao = db.productDao()
    private val orderDao = db.orderDao()
    private val categoryDao = db.categoryDao()
    private val productLocalDao = db.productLocalDao()

    suspend fun products() = productDao.getActive()
    suspend fun categories() = categoryDao.getActive()
    suspend fun productLocal() = productLocalDao.getActive()
    suspend fun productLocalAll() = productLocalDao.getAll()
    
    suspend fun saveCategory(id: String, name: String) {
        categoryDao.insert(CategoryEntity(id = id, name = name, active = true))
    }
    suspend fun saveProductLocal(id: String, categoryId: String?, name: String, priceCents: Int, active: Boolean = true) {
        productLocalDao.insert(ProductLocalEntity(id = id, categoryId = categoryId, name = name, priceCents = priceCents, active = active))
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
            paymentStatus = "UNPAID",
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
}
