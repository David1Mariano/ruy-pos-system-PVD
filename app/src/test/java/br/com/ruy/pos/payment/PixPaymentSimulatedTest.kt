package br.com.ruy.pos.payment

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import br.com.ruy.pos.data.Repository
import br.com.ruy.pos.data.local.AppDatabase
import br.com.ruy.pos.payment.pix.*
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class) @Config(sdk = [28])
class PixPaymentSimulatedTest {
    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repo: Repository
    private lateinit var settings: PixSettingsStore
    private lateinit var invoices: PixInvoiceStore
    private var order = ""
    @Before fun setup() = runBlocking {
        context = ApplicationProvider.getApplicationContext(); open()
        val prefs = context.getSharedPreferences("pix_lab", Context.MODE_PRIVATE)
        settings = PixSettingsStore(prefs); invoices = PixInvoiceStore(prefs)
        settings.save(PixAccount("Banco fictício", "Loja fictícia", "", "demo@example.invalid", PixKeyType.EMAIL, "Teste", "São Paulo", true), "Operador fictício", true)
        repo.ensureTables(5); val c = repo.openTable("T05")
        repo.addItemToComanda(c, "Coxinha", 800, 2); repo.addItemToComanda(c, "Caldo", 700, 1)
        order = repo.prepareCheckout(2300, emptyList(), emptyMap(), c)
        repo.openCashSession("Fictício", 10000); Unit
    }
    private fun open() { db = Room.databaseBuilder(context, AppDatabase::class.java, "p.db").allowMainThreadQueries().build(); repo = Repository(db) }
    @After fun cleanup() { db.close(); context.deleteDatabase("p.db") }
    private suspend fun prepare(amount: Int = 2300) = PixCheckoutService(repo.payments, settings, invoices).prepare(order, amount)
    private fun proof(ref: Long, amount: Int = 2300, receipt: String = "FICTICIO-PIX-01") = PaymentCoordinator.ManualReceiptProof(ref, order, amount, true, "Operador fictício", "FICTICIO extrato 13/11/2026", receipt, true)
    private suspend fun rejects(block: suspend () -> Unit) { var thrown = false; try { block() } catch (_: Exception) { thrown = true }; assertTrue(thrown) }
    @Test fun displayingQrPersistsPendingAndSurvivesReopenWithoutCashOrTableRelease() = runBlocking {
        val a = prepare(); val quote = invoices.load(a.attemptId)!!
        assertEquals(2300, PixBrCode.validate(quote.payload).amountCents)
        db.close(); open()
        assertEquals(a.attemptId, db.stoneDao().active()!!.attemptId)
        assertEquals("PENDENTE", repo.getOrder(order)!!.paymentStatus)
        assertEquals("OCUPADA", db.tableDao().byId("T05")!!.status)
        assertTrue(repo.getCashMovements(repo.getOpenCashSession()!!.sessionId).isEmpty())
        repo.payments.receive("ruypos-stone://pay-response?order_id=${a.stoneOrderId}&code=0")
        rejects { prepare() }; rejects { repo.payments.confirmCash(order, 2300, 2300) }
        assertEquals("AGUARDANDO", repo.getPayments(order).single().status)
    }
    @Test fun auditedProofOnlySettlesExactlyOnceAndPartialPaymentKeepsTable() = runBlocking {
        val first = prepare(1600)
        val good = proof(first.stoneOrderId, 1600)
        for (bad in listOf(good.copy(amountCents = 1599), good.copy(receipt = ""), good.copy(checked = false), good.copy(orderId = "other"))) rejects { repo.payments.confirmPix(bad) }
        (1..2).map { async(Dispatchers.IO) { repo.payments.confirmPix(good) } }.awaitAll()
        assertEquals("OCUPADA", db.tableDao().byId("T05")!!.status)
        val second = prepare(700)
        rejects { repo.payments.confirmPix(proof(second.stoneOrderId, 700)) }
        repo.payments.confirmPix(proof(second.stoneOrderId, 700, "FICTICIO-PIX-02"))
        assertEquals("LIVRE", db.tableDao().byId("T05")!!.status)
        assertEquals(2, repo.getCashMovements(repo.getOpenCashSession()!!.sessionId).size)
        assertEquals("Operador fictício", db.stoneDao().byReference(second.stoneOrderId)!!.reconciledBy)
    }
    @Test fun receiverChangesCannotMutateAnAlreadyIssuedQrAndNoReceiptRequiresAttestation() = runBlocking {
        val a = prepare(); val old = invoices.load(a.attemptId)!!
        settings.save(settings.load()!!.copy(key = "other@example.invalid"), "Operador fictício", true)
        assertEquals(old, invoices.load(a.attemptId))
        rejects { repo.payments.confirmPix(proof(a.stoneOrderId).copy(received = false, checked = false)) }
        repo.payments.confirmPix(proof(a.stoneOrderId).copy(received = false))
        assertTrue(repo.getAllPayments().none { it.status == "PAGO" })
        assertNull(db.stoneDao().active())
    }
    @Test fun closedCashAndDisabledDynamicProviderCannotCreateACharge() = runBlocking {
        repo.payments.closeCash(repo.getOpenCashSession()!!.sessionId, 10000)
        rejects { prepare() }; assertTrue(repo.getAllPayments().isEmpty())
        assertFalse(UnconfiguredDynamicPixProvider.available)
        rejects { UnconfiguredDynamicPixProvider.create(order, 2300, UUID.randomUUID().toString()) }
        rejects { UnconfiguredDynamicPixProvider.query("FICTICIO") }
    }
}
