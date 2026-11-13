package br.com.ruy.pos.payment

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import br.com.ruy.pos.data.Repository
import br.com.ruy.pos.data.local.AppDatabase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class) @Config(sdk = [28])
class VoucherPaymentSimulatedTest {
    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repo: Repository
    private var options = ExternalPaymentOptions()
    private var order = ""
    @Before fun setup() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        repo = Repository(db) { options }
        repo.openCashSession("Fictício", 10000)
        order = repo.prepareCheckout(2300, listOf("Produto fictício" to 1), mapOf("Produto fictício" to 2300))
    }
    @After fun cleanup() { db.close() }
    private suspend fun rejected(block: suspend () -> Unit) { var error: Exception? = null; try { block() } catch (e: Exception) { error = e }; assertNotNull(error) }
    @Test fun disabledOrDifferentBrandModalityCannotBeRecorded() = runBlocking {
        rejected { repo.payments.confirmExternal(order, 2300, "VOUCHER_MEAL", "Fictício", "FICTICIO extrato", transactionRef = "VCH-001") }
        options = ExternalPaymentOptions(listOf(VoucherRule("VOUCHER_FOOD", "Provedor fictício", "Bandeira fictícia")))
        rejected { repo.payments.confirmExternal(order, 2300, "VOUCHER_MEAL", "Fictício", "FICTICIO extrato", transactionRef = "VCH-001") }
        rejected { repo.payments.confirmExternal(order, 2300, "VOUCHER_FOOD", "Fictício", "FICTICIO extrato", installments = 2, transactionRef = "VCH-001") }
        assertTrue(repo.getAllPayments().isEmpty())
    }
    @Test fun enabledVoucherCapturesBrandProviderAndRepeatedOperationIsIdempotent() = runBlocking {
        options = ExternalPaymentOptions(listOf(VoucherRule("VOUCHER_MEAL", "Provedor fictício", "Bandeira fictícia")))
        val operation = UUID.randomUUID().toString()
        val results = (1..2).map { async(Dispatchers.IO) {
            repo.payments.confirmExternal(order, 2300, "VOUCHER_MEAL", "Operador fictício", "FICTICIO extrato", transactionRef = "VCH-001", operationId = operation)
        } }.awaitAll()
        assertEquals(results[0], results[1])
        assertEquals(1, repo.getAllPayments().size); assertEquals("PAGO", repo.getOrder(order)!!.paymentStatus)
        val audit = db.stoneDao().observeByOrder(order).first().single()
        assertTrue(audit.detail!!.contains("Bandeira fictícia")); assertEquals("Operador fictício", audit.reconciledBy)
        assertEquals(1, repo.getCashMovements(repo.getOpenCashSession()!!.sessionId).size)
        rejected { repo.payments.confirmExternal(order, 2200, "VOUCHER_MEAL", "Fictício", "FICTICIO extrato", transactionRef = "VCH-001", operationId = operation) }
    }
    @Test fun noReceiptNoPaymentAndClosedCashCannotReceive() = runBlocking {
        rejected { repo.payments.confirmExternal(order, 2300, "DEBIT", "Fictício", "FICTICIO extrato") }
        val session = repo.getOpenCashSession()!!
        repo.payments.closeCash(session.sessionId, 10000)
        rejected { repo.payments.confirmExternal(order, 2300, "CREDIT", "Fictício", "FICTICIO extrato", transactionRef = "EXT-001") }
        assertTrue(repo.getAllPayments().isEmpty())
    }
    @Test fun configurationNeedsExplicitAuthorizationAndPersistsOnlyEnabledRules() {
        val prefs = context.getSharedPreferences("voucher_lab", Context.MODE_PRIVATE)
        val store = PaymentCapabilitiesStore(prefs)
        assertTrue(store.load().vouchers.isEmpty())
        val rule = ExternalPaymentOptions(listOf(VoucherRule("VOUCHER_FOOD", "Provedor fictício", "Bandeira fictícia")))
        assertThrows(IllegalArgumentException::class.java) { store.save(rule, "Fictício", false) }
        store.save(rule, "Fictício", true); assertEquals(rule, store.load())
        store.save(ExternalPaymentOptions(), "Fictício", true); assertTrue(store.load().vouchers.isEmpty())
    }
}
