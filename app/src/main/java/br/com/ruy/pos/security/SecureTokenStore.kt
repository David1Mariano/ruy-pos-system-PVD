package br.com.ruy.pos.security

import android.content.Context
import android.content.SharedPreferences

class SecureTokenStore internal constructor(private val secure: SharedPreferences, private val legacy: SharedPreferences) {
    constructor(context: Context) : this(SecurePrefs.get(context), context.getSharedPreferences("ruy_pos_v1", Context.MODE_PRIVATE))
    fun read(): String {
        val current = secure.getString("device_token", null)
        if (current != null) {
            if (legacy.contains("device_token")) check(legacy.edit().remove("device_token").commit())
            return current
        }
        val old = legacy.getString("device_token", "").orEmpty()
        if (old.isNotEmpty()) write(old)
        return old
    }
    fun write(token: String) {
        check(secure.edit().putString("device_token", token).commit()) { "Não foi possível proteger a configuração" }
        check(legacy.edit().remove("device_token").commit())
    }
}
