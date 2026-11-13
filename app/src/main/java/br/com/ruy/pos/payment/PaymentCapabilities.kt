package br.com.ruy.pos.payment

import android.content.Context
import android.content.SharedPreferences
import br.com.ruy.pos.security.SecurePrefs
import org.json.JSONArray
import org.json.JSONObject

data class VoucherRule(val method: String, val provider: String, val brand: String)
data class ExternalPaymentOptions(val vouchers: List<VoucherRule> = emptyList()) {
    fun validated(): ExternalPaymentOptions {
        require(vouchers.map { it.method }.distinct().size == vouchers.size)
        vouchers.forEach { require(it.method in VOUCHER_METHODS && it.provider.trim().length in 2..80 && it.brand.trim().length in 2..60) { "Confira provedor, bandeira e modalidade autorizados" } }
        return copy(vouchers = vouchers.map { it.copy(provider = it.provider.trim(), brand = it.brand.trim()) })
    }
    companion object { val VOUCHER_METHODS = setOf("VOUCHER_FOOD", "VOUCHER_MEAL", "VOUCHER") }
}

class PaymentCapabilitiesStore internal constructor(private val prefs: SharedPreferences) {
    constructor(context: Context) : this(SecurePrefs.get(context))
    fun load(): ExternalPaymentOptions {
        val a = JSONArray(prefs.getString("voucher_rules", "[]"))
        return ExternalPaymentOptions((0 until a.length()).map { a.getJSONObject(it) }.map { VoucherRule(it.getString("method"), it.getString("provider"), it.getString("brand")) }).validated()
    }
    fun save(options: ExternalPaymentOptions, operator: String, authorizedConfirmed: Boolean) = synchronized(LOCK) {
        require(authorizedConfirmed && operator.trim().length in 3..80) { "Confirme o credenciamento e informe o responsável" }
        val valid = options.validated()
        val a = JSONArray()
        valid.vouchers.forEach { a.put(JSONObject().put("method", it.method).put("provider", it.provider).put("brand", it.brand)) }
        val audit = JSONArray(prefs.getString("voucher_changes", "[]")).put(JSONObject().put("at", System.currentTimeMillis())
            .put("operator", operator.trim()).put("methods", JSONArray(valid.vouchers.map { it.method })))
        check(prefs.edit().putString("voucher_rules", a.toString()).putString("voucher_changes", audit.toString()).commit())
    }
    companion object { private val LOCK = Any() }
}
