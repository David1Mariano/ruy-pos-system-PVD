package br.com.ruy.pos.payment.pix

import android.content.Context
import android.content.SharedPreferences
import br.com.ruy.pos.BuildConfig
import br.com.ruy.pos.security.SecurePrefs
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

/** Configuration is encrypted with Android Keystore. No secrets or account data enter diagnostics. */
class PixSettingsStore internal constructor(private val prefs: SharedPreferences) {
    constructor(context: Context) : this(SecurePrefs.get(context))
    fun load(): PixAccount? = prefs.getString("pix_profile", null)?.let { fromJson(JSONObject(it)).validated() }
    fun audits(): JSONArray = JSONArray(prefs.getString("pix_changes", "[]"))
    fun save(account: PixAccount, operator: String, confirmed: Boolean) = synchronized(LOCK) {
        require(confirmed && operator.trim().length in 3..80) { "Confirme a alteração e informe o responsável" }
        val clean = account.validated()
        if (BuildConfig.HOMOLOGATION) require(clean.keyType == PixKeyType.EMAIL && clean.key.endsWith(".invalid")) { "Use somente e-mail fictício .invalid na homologação" }
        val encoded = toJson(clean).toString()
        val previous = prefs.getString("pix_profile", "")!!
        val history = audits().put(JSONObject().put("at", System.currentTimeMillis()).put("operator", operator.trim())
            .put("previous", fingerprint(previous)).put("current", fingerprint(encoded)))
        check(prefs.edit().putString("pix_profile", encoded).putString("pix_changes", history.toString()).commit()) { "Não foi possível salvar a conta" }
    }
    companion object {
        private val LOCK = Any()
        fun toJson(a: PixAccount) = JSONObject().put("institution", a.institution).put("receiver", a.receiver).put("taxId", a.taxId)
            .put("key", a.key).put("keyType", a.keyType.name).put("label", a.label).put("city", a.city).put("registeredConfirmed", a.registeredConfirmed)
        fun fromJson(o: JSONObject) = PixAccount(o.getString("institution"), o.getString("receiver"), o.optString("taxId"), o.getString("key"),
            PixKeyType.valueOf(o.getString("keyType")), o.getString("label"), o.getString("city"), o.getBoolean("registeredConfirmed"))
        fun fingerprint(value: String): String = MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
