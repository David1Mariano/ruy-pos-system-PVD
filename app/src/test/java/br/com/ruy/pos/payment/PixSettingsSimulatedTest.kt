package br.com.ruy.pos.payment

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import br.com.ruy.pos.payment.pix.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Injected lab storage only; production constructor always uses Android Keystore encrypted preferences. */
@RunWith(RobolectricTestRunner::class) @Config(sdk = [28])
class PixSettingsSimulatedTest {
    @Test fun saveRequiresConfirmationPersistsAndAuditsWithoutLeakingKeyIntoAudit() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val prefs = context.getSharedPreferences("pix_lab", Context.MODE_PRIVATE)
        val store = PixSettingsStore(prefs)
        val a = PixAccount("Banco fictício", "Loja Fictícia", "", "demo@example.invalid", PixKeyType.EMAIL, "Teste", "São Paulo", true)
        assertNull(store.load())
        assertThrows(IllegalArgumentException::class.java) { store.save(a, "Operador fictício", false) }
        assertNull(store.load()); assertEquals(0, store.audits().length())
        store.save(a, "Operador fictício", true)
        assertEquals(a.validated(), PixSettingsStore(prefs).load())
        store.save(a.copy(label = "Teste novo"), "Operador fictício", true)
        assertEquals(2, store.audits().length())
        assertFalse(store.audits().toString().contains(a.key))
        assertEquals("Operador fictício", store.audits().getJSONObject(0).getString("operator"))
    }
}
