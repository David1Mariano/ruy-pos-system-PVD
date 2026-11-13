package br.com.ruy.pos.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import br.com.ruy.pos.data.Repository
import br.com.ruy.pos.SyncClient
import org.json.JSONObject

class SyncWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): androidx.work.ListenableWorker.Result {
        if (br.com.ruy.pos.BuildConfig.HOMOLOGATION) return androidx.work.ListenableWorker.Result.success()
        val prefs = applicationContext.getSharedPreferences("ruy_pos_v1", Context.MODE_PRIVATE)
        val url = prefs.getString("sync_url", "") ?: ""
        val token = prefs.getString("device_token", "") ?: ""
        if (!url.startsWith("https://")) return androidx.work.ListenableWorker.Result.retry()
        if (token.isBlank()) return androidx.work.ListenableWorker.Result.retry()
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
                val syncResult = SyncClient.send(url, token, obj)
                if (syncResult.isSuccess) {
                    // Idempotent success - same UUID already accepted, no duplicate
                    repo.markSynced(o.id)
                } else {
                    val exception = syncResult.exceptionOrNull()
                    val msg = exception?.message ?: ""
                    if (msg.startsWith("409_CONFLICT")) {
                        // Same UUID with different payload - conflict, do not mark synced
                        // Leave as PENDING for retry with correct payload
                        return androidx.work.ListenableWorker.Result.retry()
                    } else {
                        // Other error - retry
                        return androidx.work.ListenableWorker.Result.retry()
                    }
                }
            } catch (e: Exception) {
                return androidx.work.ListenableWorker.Result.retry()
            }
        }
        return androidx.work.ListenableWorker.Result.success()
    }
}
