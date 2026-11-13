package br.com.ruy.pos

import android.content.Context
import android.content.ContextWrapper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.ruy.pos.data.Repository
import br.com.ruy.pos.data.local.AppDatabase
import br.com.ruy.pos.payment.PaymentCoordinator
import br.com.ruy.pos.payment.pix.*
import br.com.ruy.pos.security.SecureTokenStore
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

/** Native Keystore/crypto tests, using dedicated preference files, no production settings or network. */
@RunWith(AndroidJUnit4::class)
class ProtectedPaymentsInstrumentedTest {
    private lateinit var context: Context
    @Before fun setup() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        check(BuildConfig.HOMOLOGATION && base.packageName == "br.com.ruy.pos.homologation")
        context = object : ContextWrapper(base) {
            override fun getApplicationContext(): Context = this
            override fun getSharedPreferences(name: String, mode: Int) = base.getSharedPreferences("payment_fixture_$name", mode)
        }
    }
    @After fun cleanup() {
        listOf("ruy_pos_secure", "ruy_pos_v1").forEach { context.getSharedPreferences(it, Context.MODE_PRIVATE).edit().clear().commit() }
    }
    private fun account() = PixAccount("Banco fictício", "Loja fictícia", "", "demo@example.invalid", PixKeyType.EMAIL, "Teste", "São Paulo", true)
    @Test fun accountIsEncryptedAtRestAndCanBeReadAfterReopeningStore() {
        val settings = PixSettingsStore(context)
        settings.save(account(), "Operador fictício", true)
        assertEquals(account().validated(), PixSettingsStore(context).load())
        val raw = context.getSharedPreferences("ruy_pos_secure", Context.MODE_PRIVATE).all.toString()
        assertFalse(raw.contains(account().key)); assertFalse(raw.contains("pix_profile"))
    }
    @Test fun legacyFictitiousTokenMovesToEncryptedPreferences() {
        val old = context.getSharedPreferences("ruy_pos_v1", Context.MODE_PRIVATE)
        old.edit().putString("device_token", "FICTICIO-NOT-A-CREDENTIAL").commit()
        assertEquals("FICTICIO-NOT-A-CREDENTIAL", SecureTokenStore(context).read())
        assertFalse(old.contains("device_token"))
        assertFalse(context.getSharedPreferences("ruy_pos_secure", Context.MODE_PRIVATE).all.toString().contains("FICTICIO-NOT-A-CREDENTIAL"))
    }
    @Test fun encryptedQrSnapshotAndManualReceiptIntegrateWithNativeRoom() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        try {
            val repo = Repository(db); repo.openCashSession("Fictício", 10000)
            val order = repo.prepareCheckout(2300, listOf("Fictício" to 1), mapOf("Fictício" to 2300))
            val settings = PixSettingsStore(context); settings.save(account(), "Operador fictício", true)
            val invoices = PixInvoiceStore(context)
            val attempt = PixCheckoutService(repo.payments, settings, invoices).prepare(order, 2300)
            assertEquals(2300, PixBrCode.validate(invoices.load(attempt.attemptId)!!.payload).amountCents)
            assertEquals("PENDENTE", repo.getOrder(order)!!.paymentStatus)
            assertTrue(repo.getCashMovements(repo.getOpenCashSession()!!.sessionId).isEmpty())
            repo.payments.confirmPix(PaymentCoordinator.ManualReceiptProof(attempt.stoneOrderId, order, 2300, true,
                "Operador fictício", "FICTICIO extrato 13/11/2026", "FICTICIO-PIX-01", true))
            assertEquals("PAGO", repo.getOrder(order)!!.paymentStatus)
            assertEquals(2300, repo.payments.closeCash(repo.getOpenCashSession()!!.sessionId, 10000).totalPixCents)
        } finally { db.close() }
    }
}
