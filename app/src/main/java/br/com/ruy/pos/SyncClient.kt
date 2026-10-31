package br.com.ruy.pos

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Confirma somente aceite durável pelo servidor; não implica pagamento aprovado. */
object SyncClient {
    fun send(endpoint: String, bearer: String, order: JSONObject): Boolean {
        require(endpoint.startsWith("https://"))
        require(bearer.isNotBlank())
        val id = order.getString("id")
        val conn = (URL(endpoint).openConnection() as HttpURLConnection)
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.doOutput = true
            conn.instanceFollowRedirects = false
            conn.setRequestProperty("Authorization", "Bearer $bearer")
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.setRequestProperty("Idempotency-Key", id)
            conn.outputStream.use { it.write(order.toString().toByteArray(Charsets.UTF_8)) }
            val status = conn.responseCode
            if (status !in 200..299) throw IllegalStateException("HTTP $status")
            val reply = conn.inputStream.bufferedReader().use { it.readText() }
            val obj = JSONObject(reply)
            return obj.optString("id") == id && obj.optBoolean("persisted", false)
        } finally { conn.disconnect() }
    }
}
