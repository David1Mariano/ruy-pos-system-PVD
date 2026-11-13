package br.com.ruy.pos

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Envia pedidos ao backend com idempotencia controlada via header Idempotency-Key.
 *
 * Comportamento conforme contrato:
 * - Mesmo UUID + mesmo corpo  -> retorna acele anterior, sem duplicata (200/201).
 * - Mesmo UUID + corpo diferente -> rejeita com 409 Conflict.
 * - Requisicao em processamento pode receber 409 transitorio, que deve ser tratado como temporario.
 */
object SyncClient {
    fun send(endpoint: String, bearer: String, order: JSONObject): Result<String> {
        if (BuildConfig.HOMOLOGATION) return Result.failure(IllegalStateException("Sincronização bloqueada em homologação"))
        return if (!endpoint.startsWith("https://")) Result.failure(Exception("https_required"))
        else if (bearer.isBlank()) Result.failure(Exception("token_required"))
        else {
            val id = order.getString("id")
            return try {
                val conn = URL(endpoint).openConnection() as HttpURLConnection
                try {
                    conn.requestMethod = "POST"
                    conn.connectTimeout = 10000
                    conn.readTimeout = 10000
                    conn.doOutput = true
                    conn.instanceFollowRedirects = false
                    conn.setRequestProperty("Authorization", "Bearer " + bearer)
                    conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    conn.setRequestProperty("Idempotency-Key", id)
                    conn.outputStream.use { it.write(order.toString().toByteArray(Charsets.UTF_8)) }
                    val status = conn.responseCode

                    // Tratamento de idempotencia conforme contrato HTTP 409
                    if (status == 409) {
                        // Conflito - mesmo UUID com payload diferente
                        val errorReply = conn.inputStream.bufferedReader().use { it.readText() }
                        Result.failure(Exception("409_CONFLICT:" + errorReply))
                    } else if (status < 200 || status >= 300) {
                        Result.failure(Exception("HTTP " + status))
                    } else {
                        val reply = conn.inputStream.bufferedReader().use { it.readText() }
                        val obj = JSONObject(reply)
                        val respId = obj.optString("id", "")
                        val persisted = obj.optBoolean("persisted", false)
                        if (respId == id && persisted) {
                            Result.success(reply)
                        } else {
                            Result.failure(Exception("ID mismatch or not persisted: " + respId + " / " + persisted))
                        }
                    }
                } finally { conn.disconnect() }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
