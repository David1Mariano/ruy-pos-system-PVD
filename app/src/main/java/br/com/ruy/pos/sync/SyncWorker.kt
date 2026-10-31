package br.com.ruy.pos.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import br.com.ruy.pos.data.Repository
import br.com.ruy.pos.SyncClient
import org.json.JSONObject

class SyncWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("ruy_pos_v1", Context.MODE_PRIVATE)
        val url = prefs.getString("sync_url", "") ?: ""
        val token = prefs.getString("device_token", "") ?: ""
        if (!url.startsWith("https://")) return Result.retry()
        if (token.isBlank()) return Result.retry()
        val repo = Repository(applicationContext)
        val pending = repo.pendingOrders()
        for (o in pending) {
            try {
                val obj = JSONObject()
                    .put("id", o.id)
                    .put("createdAt", o.createdAt)
                    .put("total_cents", o.totalCents)
                    .put("payment_status", o.paymentStatus)
                    .put("syncStatus", "PENDING")
                    .put("items", org.json.JSONArray(o.itemsJson))
                val ok = SyncClient.send(url, token, obj)
                if (ok) repo.markSynced(o.id)
            } catch (e: Exception) {
                return Result.retry()
            }
        }
        return Result.success()
    }
}
