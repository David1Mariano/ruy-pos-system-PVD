package br.com.ruy.pos.payment

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import br.com.ruy.pos.security.SecureTokenStore
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class) @Config(sdk = [28])
class SecureTokenStoreSimulatedTest {
    @Test fun legacyTokenIsRemovedOnlyAfterSuccessfulProtectedWrite() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val old = context.getSharedPreferences("old_lab", Context.MODE_PRIVATE)
        val safe = context.getSharedPreferences("safe_lab", Context.MODE_PRIVATE)
        old.edit().putString("device_token", "FICTICIO-NOT-A-CREDENTIAL").commit()
        val failing = object : SharedPreferences by safe {
            override fun edit(): SharedPreferences.Editor {
                val delegate = safe.edit()
                return object : SharedPreferences.Editor by delegate {
                    override fun putString(key: String?, value: String?): SharedPreferences.Editor { delegate.putString(key, value); return this }
                    override fun commit() = false
                }
            }
        }
        assertThrows(IllegalStateException::class.java) { SecureTokenStore(failing, old).read() }
        assertTrue(old.contains("device_token"))
        assertEquals("FICTICIO-NOT-A-CREDENTIAL", SecureTokenStore(safe, old).read())
        assertFalse(old.contains("device_token")); assertTrue(safe.contains("device_token"))
    }
}
