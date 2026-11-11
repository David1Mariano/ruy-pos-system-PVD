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
import java.util.UUID

/** SIMULATED Android/SQLite. No Stone app, terminal, backend or real charge is used. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PaymentCoordinatorSimulatedTest {
    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var coordinator: PaymentCoordinator
    private lateinit var name: String
    private val orderId = UUID.randomUUID().toString()
    private val sessionId = "SIMULATED-SESSION"
    private val comandaId = "SIMULATED-COMANDA"

    @Before fun setup() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        // Keep SQLite sidecar paths below Windows MAX_PATH in Robolectric's per-method temp dirs.
        name = "p.db"
        open()
        db.orderDao().insert(OrderEntity(orderId, 10000, "NAO_PAGO", "PENDING", "[]", 1, null, null, null, comandaId))
        db.tableDao().upsertTable(TableEntity("T01", 1, "OCUPADA"))
        db.tableOrderDao().insert(TableOrderEntity(comandaId, "T01", "ABERTA", 10000))
        db.tableOrderItemDao().insert(TableOrderItemEntity("SIMULATED-ITEM", comandaId, null, "Item", 10000, 1))
        db.cashSessionDao().insert(CashSessionEntity(sessionId, "Operador simulado", 1, openingBalanceCents = 2000, status = "ABERTO"))
    }
    private fun open() {
        db = Room.databaseBuilder(context, AppDatabase::class.java, name).allowMainThreadQueries().build()
        coordinator = PaymentCoordinator(db)
    }
    @After fun cleanup() { db.close(); context.deleteDatabase(name) }

    private suspend fun fails(block: suspend () -> Unit) {
        var failure: Throwable? = null
        try { block() } catch (e: Exception) { failure = e }
        assertNotNull("Expected rejection", failure)
    }
    private suspend fun attempt(amount: Int = 10000, method: String = "DEBIT", count: Int = 1, type: String = "NONE"): StoneAttemptEntity {
        val a = coordinator.createAttempt(orderId, amount, method, count, type)
        assertTrue(coordinator.claimLaunch(a.stoneOrderId))
        return a
    }
    private fun callback(a: StoneAttemptEntity, amount: Int = 10000, method: String = "DEBIT", atk: String = "SIMULATED-ATK") =
        "ruypos-stone://pay-response?code=0&order_id=${a.stoneOrderId}&amount=$amount&type=$method&installment_count=0&atk=$atk&itk=SIMULATED-ITK&authorization_code=123456&authorization_date_time=04%2F11%2F2026"
    private suspend fun proof(a: StoneAttemptEntity, received: Boolean = true, atk: String = "SIMULATED-ATK"): PaymentCoordinator.Reconciliation {
        val p = requireNotNull(db.paymentDao().byId(a.paymentId))
        return PaymentCoordinator.Reconciliation(a.stoneOrderId, p.amountCents, p.method, p.installments, received,
            "Operador simulado", "SIMULADO extrato 04/11/2026 12:00", true, atk, "123456")
    }
    private suspend fun noMoneyOrFreeTable() {
        assertTrue(db.cashMovementDao().getBySession(sessionId).isEmpty())
        assertEquals("OCUPADA", db.tableDao().byId("T01")!!.status)
        assertEquals("ABERTA", db.tableOrderDao().byId(comandaId)!!.status)
        assertTrue(db.paymentDao().getByOrder(orderId).none { it.status == "PAGO" })
    }

    @Test fun approvedReturnPersistsEvidenceButCannotSettleMoney() = runBlocking {
        val a = attempt()
        coordinator.receive(callback(a))
        assertEquals("EM_CONCILIACAO", db.stoneDao().active()!!.state)
        assertEquals("INDETERMINADO", db.paymentDao().byId(a.paymentId)!!.status)
        assertEquals("SIMULATED-ATK", db.stoneDao().active()!!.atk)
        noMoneyOrFreeTable()
        fails { coordinator.createAttempt(orderId, 10000, "DEBIT") }
    }
    @Test fun reconciliationAndCallbackRepeatsCountCashExactlyOnce() = runBlocking {
        val a = attempt()
        coordinator.receive(callback(a))
        val input = proof(a)
        coordinator.reconcile(input)
        coordinator.reconcile(input)
        repeat(3) { coordinator.receive(callback(a)) }
        coordinator.receive(callback(a) + "&success=true")
        assertEquals(1, db.cashMovementDao().getBySession(sessionId).size)
        assertEquals(10000, db.cashMovementDao().getBySession(sessionId).single().amountCents)
        assertEquals("PAGO", db.paymentDao().byId(a.paymentId)!!.status)
        assertEquals("SIMULATED-ATK", db.orderDao().byId(orderId)!!.stoneTransactionId)
        assertEquals("PAGO", db.orderDao().byId(orderId)!!.paymentStatus)
        assertEquals("FECHADA", db.tableOrderDao().byId(comandaId)!!.status)
        assertEquals("LIVRE", db.tableDao().byId("T01")!!.status)
        assertNull(db.stoneDao().active())
        assertEquals(2, db.stoneDao().events().size)
        fails { coordinator.confirmCash(orderId, 10000, 10000) }
    }
    @Test fun officialDeclineDoesNotRecordCashOrAllowAutomaticRetry() = runBlocking {
        val a = attempt()
        coordinator.receive("ruypos-stone://pay-response?order_id=${a.stoneOrderId}&success=false&reason=1016&response_code=-6&code=-6")
        assertEquals("RECUSADO", db.stoneDao().active()!!.state)
        noMoneyOrFreeTable()
        fails { coordinator.createAttempt(orderId, 10000, "DEBIT") }
        coordinator.reconcile(proof(a, received = false))
        val next = coordinator.createAttempt(orderId, 10000, "DEBIT")
        assertTrue(next.stoneOrderId > a.stoneOrderId)
        assertNotEquals(a.attemptId, next.attemptId)
        assertNotEquals(a.paymentId, next.paymentId)
        noMoneyOrFreeTable()
    }
    @Test fun cancellationRequiresStatementBeforeRetry() = runBlocking {
        val a = attempt()
        coordinator.receive("ruypos-stone://pay-response?code=2&order_id=${a.stoneOrderId}")
        assertEquals("CANCELADO", db.paymentDao().byId(a.paymentId)!!.status)
        noMoneyOrFreeTable()
        fails { coordinator.confirmCash(orderId, 10000, 10000) }
    }
    @Test fun uncorrelatedExitCannotFinalizeActiveAttempt() = runBlocking {
        attempt()
        coordinator.receive("ruypos-stone://pay-response?code=2")
        assertEquals("INDETERMINADO", db.stoneDao().active()!!.state)
        noMoneyOrFreeTable()
    }
    @Test fun invalidAndUnknownReturnAreAuditedWithoutMutatingFlight() = runBlocking {
        val a = attempt()
        coordinator.receive(callback(a).replace("ruypos-stone:", "evil:"))
        coordinator.receive(callback(a).replace("order_id=${a.stoneOrderId}", "order_id=9223372036854775807"))
        coordinator.receive(callback(a) + "&amount=10000")
        assertEquals("AGUARDANDO", db.stoneDao().active()!!.state)
        assertEquals(3, db.stoneDao().events().size)
        noMoneyOrFreeTable()
    }
    @Test fun divergentValueNeverSettles() = runBlocking {
        val a = attempt()
        coordinator.receive(callback(a, amount = 9999))
        assertEquals("INDETERMINADO", db.stoneDao().active()!!.state)
        noMoneyOrFreeTable()
    }
    @Test fun zeroAloneIsNotProofEvenWithReference() = runBlocking {
        val a = attempt()
        coordinator.receive("ruypos-stone://pay-response?code=0&order_id=${a.stoneOrderId}")
        noMoneyOrFreeTable()
        assertNotNull(db.stoneDao().active())
    }
    @Test fun absentReturnTimesOutAndKeepsLockAndOrder() = runBlocking {
        val a = attempt()
        coordinator.expireMissingReturn(a.createdAt + StoneProtocol.RETURN_TIMEOUT_MS - 1)
        assertEquals("AGUARDANDO", db.stoneDao().active()!!.state)
        coordinator.expireMissingReturn(a.createdAt + StoneProtocol.RETURN_TIMEOUT_MS)
        assertEquals("INDETERMINADO", db.stoneDao().active()!!.state)
        assertNotNull(db.orderDao().byId(orderId))
        fails { coordinator.createAttempt(orderId, 10000, "PIX") }
        fails { coordinator.closeCash(sessionId, 2000) }
        noMoneyOrFreeTable()
    }
    @Test fun realSqliteReopenRecoversFlightAndPaymentAfterSimulatedProcessDeath() = runBlocking {
        val a = attempt()
        db.close()
        open()
        val restored = coordinator.observeActive().first()!!
        assertEquals(a.attemptId, restored.attemptId)
        assertEquals(a.paymentId, restored.paymentId)
        assertEquals(a.stoneOrderId, restored.stoneOrderId)
        assertEquals(10000, coordinator.request(restored).amountCents)
        assertFalse(coordinator.claimLaunch(a.stoneOrderId))
        fails { coordinator.createAttempt(orderId, 10000, "DEBIT") }
        coordinator.receive(callback(a))
        coordinator.reconcile(proof(a))
        assertEquals(1, db.cashMovementDao().getBySession(sessionId).size)
    }
    @Test fun crashBetweenPersistenceAndLaunchAlsoKeepsFlight() = runBlocking {
        val a = coordinator.createAttempt(orderId, 10000, "DEBIT")
        db.close(); open()
        assertEquals("CRIADA", db.stoneDao().active()!!.state)
        fails { coordinator.createAttempt(orderId, 10000, "DEBIT") }
        coordinator.expireMissingReturn(a.createdAt + StoneProtocol.RETURN_TIMEOUT_MS)
        assertEquals("INDETERMINADO", db.stoneDao().active()!!.state)
    }
    @Test fun simultaneousClicksProduceOnlyOnePersistedAttempt() = runBlocking {
        val results = (1..2).map { async(Dispatchers.IO) { runCatching { coordinator.createAttempt(orderId, 10000, "DEBIT") } } }.awaitAll()
        assertEquals(1, results.count { it.isSuccess })
        assertEquals(1, db.paymentDao().getByOrder(orderId).size)
        val a = db.stoneDao().active()!!
        assertTrue(coordinator.claimLaunch(a.stoneOrderId))
        assertFalse(coordinator.claimLaunch(a.stoneOrderId))
    }
    @Test fun terminalLockAlsoBlocksAnotherOrder() = runBlocking {
        attempt()
        val other = orderId + "-other"
        db.orderDao().insert(OrderEntity(other, 10000, "NAO_PAGO", "PENDING", "[]", 2, null, null, null))
        fails { coordinator.createAttempt(other, 10000, "PIX") }
    }
    @Test fun partialPaymentKeepsTableUntilLastCent() = runBlocking {
        val first = attempt(4000)
        coordinator.reconcile(proof(first, atk = "SIMULATED-ATK-1"))
        assertEquals("PARCIAL", db.orderDao().byId(orderId)!!.paymentStatus)
        assertEquals("OCUPADA", db.tableDao().byId("T01")!!.status)
        assertEquals("ABERTA", db.tableOrderDao().byId(comandaId)!!.status)
        fails { coordinator.createAttempt(orderId, 6001, "DEBIT") }
        val second = attempt(6000)
        coordinator.reconcile(proof(second, atk = "SIMULATED-ATK-2"))
        assertEquals("LIVRE", db.tableDao().byId("T01")!!.status)
        assertEquals(10000, db.cashMovementDao().getBySession(sessionId).sumOf { it.amountCents })
    }
    @Test fun wrongProofOrUnacknowledgedStatementCannotSettle() = runBlocking {
        val a = attempt()
        val input = proof(a)
        for (bad in listOf(input.copy(amountCents = 10001), input.copy(method = "CREDIT"), input.copy(installments = 2),
            input.copy(checkedStatement = false), input.copy(operator = ""), input.copy(evidence = ""), input.copy(atk = ""), input.copy(authorizationCode = "")))
            fails { coordinator.reconcile(bad) }
        noMoneyOrFreeTable()
    }
    @Test fun atkReuseCannotCountSameTransactionTwice() = runBlocking {
        val first = attempt(4000)
        coordinator.reconcile(proof(first))
        val second = attempt(6000)
        coordinator.receive(callback(second, amount = 6000))
        assertEquals("INDETERMINADO", db.stoneDao().active()!!.state)
        fails { coordinator.reconcile(proof(second)) }
        assertEquals(1, db.cashMovementDao().getBySession(sessionId).size)
        assertEquals("OCUPADA", db.tableDao().byId("T01")!!.status)
    }
    @Test fun confirmedOpeningFailureReleasesOnlyUnstartedAttempt() = runBlocking {
        val a = attempt()
        coordinator.launchFailed(a.stoneOrderId, definitelyNotStarted = true)
        assertNull(db.stoneDao().active())
        assertEquals("NAO_INICIADO", db.paymentDao().byId(a.paymentId)!!.status)
        coordinator.receive(callback(a))
        assertEquals("NAO_INICIADO", db.stoneDao().byReference(a.stoneOrderId)!!.state)
        noMoneyOrFreeTable()
    }
    @Test fun ambiguousOpeningFailureKeepsLock() = runBlocking {
        val a = attempt()
        coordinator.launchFailed(a.stoneOrderId, definitelyNotStarted = false)
        assertEquals("INDETERMINADO", db.stoneDao().active()!!.state)
        fails { coordinator.createAttempt(orderId, 10000, "DEBIT") }
    }
    @Test fun callbackRacingLaunchFailureCannotBeOverwritten() = runBlocking {
        val a = attempt()
        coordinator.receive(callback(a))
        coordinator.launchFailed(a.stoneOrderId, definitelyNotStarted = true)
        assertEquals("EM_CONCILIACAO", db.stoneDao().active()!!.state)
    }
    @Test @Config(sdk = [23]) fun cashAndPixAndCreditClosingTotalsAreSeparated() = runBlocking {
        coordinator.confirmCash(orderId, 1000, 1500)
        val pix = attempt(3000, "PIX")
        coordinator.reconcile(proof(pix, atk = "SIMULATED-PIX"))
        val credit = attempt(6000, "CREDIT", 3, "MERCHANT")
        coordinator.reconcile(proof(credit, atk = "SIMULATED-CREDIT"))
        val closing = coordinator.closeCash(sessionId, 3000)
        assertEquals(10000, closing.totalSalesCents)
        assertEquals(1000, closing.totalCashCents)
        assertEquals(3000, closing.totalPixCents)
        assertEquals(6000, closing.totalCreditCents)
        assertEquals(0, closing.totalDebitCents)
        assertEquals(3000, closing.expectedCashCents)
        assertEquals(0, closing.differenceCents)
        fails { coordinator.closeCash(sessionId, 3000) }
    }
    @Test fun occupiedTableCannotBeResetByEnsureTables() = runBlocking {
        val repo = Repository(db)
        repo.ensureTables(10)
        repo.ensureTables(10)
        assertEquals("OCUPADA", db.tableDao().byId("T01")!!.status)
        fails { repo.markTableFree("T01") }
        fails { repo.closeComanda(comandaId) }
    }
    @Test fun checkoutReentryKeepsOriginalOrderForComanda() = runBlocking {
        assertEquals(orderId, Repository(db).prepareCheckout(10000, emptyList(), emptyMap(), comandaId))
        assertEquals(1, db.orderDao().all().size)
        attempt()
        fails { Repository(db).addItemToComanda(comandaId, "Extra", 100, 1) }
    }
    @Test fun unchargedDraftCanGrowAndKeepHistoricalPrices() = runBlocking {
        Repository(db).addItemToComanda(comandaId, "Extra", 100, 1)
        assertEquals(10100, db.orderDao().byId(orderId)!!.totalCents)
        assertEquals(10100, db.tableOrderDao().byId(comandaId)!!.totalCents)
        assertTrue(db.orderDao().byId(orderId)!!.itemsJson.contains("Extra"))
    }
    @Test fun legacyPendingPaymentBlocksChargesAndCanBeManuallyRecoveredWithoutRelaunch() = runBlocking {
        val legacy = PaymentEntity("SIMULATED-LEGACY", orderId, 10000, "PIX", "AGUARDANDO")
        db.paymentDao().insert(legacy)
        fails { coordinator.createAttempt(orderId, 10000, "PIX") }
        val recovered = coordinator.recoverLegacyPayment(legacy.paymentId)
        assertEquals("LEGACY_MANUAL", recovered.origin)
        assertFalse(coordinator.claimLaunch(recovered.stoneOrderId))
        noMoneyOrFreeTable()
        coordinator.reconcile(proof(recovered, atk = "SIMULATED-LEGACY-ATK"))
        assertEquals(1, db.cashMovementDao().getBySession(sessionId).size)
    }
    @Test fun closedCashCannotStartCharge() = runBlocking {
        coordinator.closeCash(sessionId, 2000)
        fails { coordinator.createAttempt(orderId, 10000, "DEBIT") }
        assertTrue(db.paymentDao().getByOrder(orderId).isEmpty())
    }
    @Test fun settlementFailureRollsBackPaymentLedgerAndTable() = runBlocking {
        val a = attempt()
        // Simulate unexpected legacy/external tampering with the cash session.
        db.cashSessionDao().close(sessionId, 2)
        fails { coordinator.reconcile(proof(a)) }
        assertEquals("AGUARDANDO", db.stoneDao().active()!!.state)
        noMoneyOrFreeTable()
    }
    @Test fun closingOverflowCannotProduceNegativeSalesOrCloseCash() = runBlocking {
        repeat(3) { index -> db.cashMovementDao().insert(CashMovementEntity("SIMULATED-OVERFLOW-$index", sessionId,
            "VENDA_CREDIT", 1000000000, "SIMULATED historical ledger", orderId)) }
        fails { coordinator.closeCash(sessionId, 2000) }
        assertNotNull(db.cashSessionDao().getOpen())
        assertTrue(db.cashClosingDao().getAll().isEmpty())
    }
}
