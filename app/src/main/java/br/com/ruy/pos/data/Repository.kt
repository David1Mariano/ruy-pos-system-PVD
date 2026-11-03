package br.com.ruy.pos.data

import android.content.Context
import br.com.ruy.pos.data.local.AppDatabase
import br.com.ruy.pos.data.local.OrderEntity
import br.com.ruy.pos.data.local.ProductEntity
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class Repository(context: Context) {
    private val db = AppDatabase.get(context)
    private val productDao = db.productDao()
    private val orderDao = db.orderDao()

    suspend fun products() = productDao.getActive()

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
