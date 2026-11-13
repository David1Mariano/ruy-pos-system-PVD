package br.com.ruy.pos.payment

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import br.com.ruy.pos.data.Repository
import br.com.ruy.pos.data.local.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Fictitious SQLite ledger only. No financial application or network is invoked. */
@RunWith(RobolectricTestRunner::class) @Config(sdk = [28])
class TapStoneExternalSimulatedTest {
    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repo: Repository
    private val p get() = repo.payments
    private var order = ""
    @Before fun setup() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        open()
        repo.ensureTables(5)
        val comanda = repo.openTable("T05")
        repo.addItemToComanda(comanda, "Coxinha", 800, 2)
        repo.addItemToComanda(comanda, "Caldo de Cana", 700, 1)
        order = repo.prepareCheckout(2300, emptyList(), emptyMap(), comanda)
        repo.openCashSession("Operador fictício", 10000)
        Unit
    }
    private fun open() {
        db = Room.databaseBuilder(context, AppDatabase::class.java, "t.db").allowMainThreadQueries().build()
        repo = Repository(db)
    }
    @After fun cleanup() { db.close(); context.deleteDatabase("t.db") }
    private suspend fun rejected(block: suspend () -> Unit) {
        var error: Exception? = null
        try { block() } catch (e: Exception) { error = e }
        assertNotNull("Must reject without settlement", error)
    }
    private suspend fun begin(amount: Int = 2300) = p.beginTapStone(order, amount, "TAPSTONE_DEBIT")
    private fun proof(a: StoneAttemptEntity, amount: Int = 2300, receipt: String = "FICTICIO-RECIBO-001") =
        PaymentCoordinator.TapStoneProof(a.stoneOrderId, order, amount, "TAPSTONE_DEBIT", true,
            "Operador fictício", "FICTICIO extrato 13/11/2026 10:00", receipt, true)
    private suspend fun notSettled() {
        assertEquals("OCUPADA", db.tableDao().byId("T05")!!.status)
        assertTrue(db.cashMovementDao().getBySession(repo.getOpenCashSession()!!.sessionId).isEmpty())
        assertTrue(repo.getPayments(order).none { it.status == "PAGO" })
    }
    @Test fun abandonAndArbitraryCallbackNeverConfirmEvenAfterTimeoutAndRestart() = runBlocking {
        val a = begin()
        p.receive("ruypos-stone://pay-response?code=0&order_id=${a.stoneOrderId}&amount=2300&type=DEBIT&atk=FAKE")
        p.expireMissingReturn(Long.MAX_VALUE)
        db.close(); open()
        assertEquals("TAPSTONE_EXTERNAL", db.stoneDao().active()!!.origin)
        assertEquals("EM_CONFERENCIA", db.stoneDao().active()!!.state)
        notSettled()
        rejected { begin() }
        rejected { p.confirmCash(order, 2300, 2300) }
    }
    @Test fun missingReceiptWrongOrderAmountMethodOrAcknowledgmentCannotSettle() = runBlocking {
        val a = begin(); val valid = proof(a)
        for (bad in listOf(valid.copy(receipt = ""), valid.copy(orderId = "outro"), valid.copy(amountCents = 2299),
            valid.copy(method = "TAPSTONE_CREDIT"), valid.copy(checked = false), valid.copy(operator = ""), valid.copy(evidence = "")))
            rejected { p.confirmTapStone(bad) }
        notSettled()
    }
    @Test fun explicitProofAndRepeatedConcurrentConfirmationSettleExactlyOnce() = runBlocking {
        val a = begin()
        (1..2).map { async(Dispatchers.IO) { p.confirmTapStone(proof(a)) } }.awaitAll()
        p.confirmTapStone(proof(a))
        assertEquals(1, repo.getAllPayments().count { it.status == "PAGO" })
        assertEquals(1, repo.getCashMovements(repo.getOpenCashSession()!!.sessionId).size)
        assertEquals("LIVRE", db.tableDao().byId("T05")!!.status)
        val audit = db.stoneDao().byReference(a.stoneOrderId)!!
        assertEquals("Operador fictício", audit.reconciledBy)
        assertTrue(audit.reconciliationEvidence!!.startsWith("MANUAL_TAPSTONE:"))
        rejected { p.confirmTapStone(proof(a).copy(receipt = "DIVERGENTE")) }
    }
    @Test fun declinedPaymentCanOnlyReleaseLockAfterExplicitNoReceiptStatement() = runBlocking {
        val a = begin()
        rejected { p.confirmTapStone(proof(a).copy(received = false, checked = false)) }
        p.confirmTapStone(proof(a).copy(received = false, receipt = ""))
        notSettled()
        assertNull(db.stoneDao().active())
        assertNotEquals(a.stoneOrderId, begin().stoneOrderId)
    }
    @Test fun partialPaymentAndDuplicateReceiptCannotReleaseTable() = runBlocking {
        val first = begin(1600); p.confirmTapStone(proof(first, 1600))
        assertEquals("PARCIAL", repo.getOrder(order)!!.paymentStatus)
        assertEquals("OCUPADA", db.tableDao().byId("T05")!!.status)
        val second = begin(700)
        rejected { p.confirmTapStone(proof(second, 700)) }
        p.confirmTapStone(proof(second, 700, "FICTICIO-RECIBO-002"))
        assertEquals("LIVRE", db.tableDao().byId("T05")!!.status)
        val closing = p.closeCash(repo.getOpenCashSession()!!.sessionId, 10000)
        assertEquals(2300, closing.totalDebitCents)
        assertEquals(0, closing.totalCashCents)
    }
    @Test fun closedCashAndCancelledOrderRejectAttemptsAndSettlement() = runBlocking {
        db.orderDao().update(repo.getOrder(order)!!.copy(paymentStatus = "CANCELADO"))
        rejected { begin() }; rejected { p.confirmCash(order, 2300, 2300) }
        db.orderDao().update(repo.getOrder(order)!!.copy(paymentStatus = "NAO_PAGO"))
        val session = repo.getOpenCashSession()!!
        p.closeCash(session.sessionId, 10000)
        rejected { begin() }
        rejected { repo.addCashMovement(session.sessionId, "SANGRIA", 2000, "Fictício") }
    }
    @Test fun cashSaleWithdrawalClosingAndReopenMatch103ReaisWithoutDuplicates() = runBlocking {
        val session = repo.getOpenCashSession()!!
        p.confirmCash(order, 2300, 2300)
        repo.addCashMovement(session.sessionId, "SANGRIA", 2000, "Fictício")
        val closing = p.closeCash(session.sessionId, 10300)
        assertEquals(10300, closing.expectedCashCents); assertEquals(0, closing.differenceCents)
        assertEquals(2300, closing.totalCashCents); assertEquals(2300, closing.totalSalesCents)
        db.close(); open()
        assertEquals(1, repo.allOrders().size); assertEquals(1, repo.getAllPayments().size)
        assertEquals(closing, repo.getCashClosings().single())
        assertEquals("LIVRE", db.tableDao().byId("T05")!!.status)
    }
    @Test fun externalCardNormalizesMethodAndPersistsOperatorEvidence() = runBlocking {
        p.confirmExternal(order, 2300, "DEBITO", "Fictício", "FICTICIO comprovante 13/11/2026", transactionRef = "EXT-TEST-01")
        assertEquals("DEBIT", repo.getPayments(order).single().method)
        val audit = db.stoneDao().observeByOrder(order).first().single()
        assertEquals("EXTERNAL_MANUAL", audit.origin)
        assertEquals("Fictício", audit.reconciledBy)
        assertEquals("FICTICIO comprovante 13/11/2026", audit.reconciliationEvidence)
        val closing = p.closeCash(repo.getOpenCashSession()!!.sessionId, 10000)
        assertEquals(2300, closing.totalDebitCents)
    }
    @Test fun productionBuildRejectsSimulatedPaymentAndDefaultSmartPosRemainsDisabled() = runBlocking {
        if (!br.com.ruy.pos.BuildConfig.HOMOLOGATION) rejected { p.confirmSimulated(order, 2300) }
        assertFalse(StonePaymentGateway(context, StoneConfiguration(true, true)).isAvailable())
        assertEquals("NAO_INICIADO", StonePaymentGateway(context).process(PaymentRequest(order, 2300, "DEBIT", reference = "1")).status)
        notSettled()
    }
    @Test fun readinessRequiresAndroidNfcEnabledAndValidatedInternetTogether() {
        val ready = TapStoneReadiness("Qualquer fabricante/modelo", true, true, true, true)
        assertTrue(ready.locallyReady)
        assertFalse(ready.copy(androidSupported = false).locallyReady)
        assertFalse(ready.copy(nfcPresent = false).locallyReady)
        assertFalse(ready.copy(nfcEnabled = false).locallyReady)
        assertFalse(ready.copy(internetValidated = false).locallyReady)
    }
}
