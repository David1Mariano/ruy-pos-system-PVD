package br.com.ruy.pos.payment

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import br.com.ruy.pos.data.local.*
import br.com.ruy.pos.ui.activity.PosActivity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** SIMULATED cold/warm Activity callbacks. No real payment app is invoked. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class StoneActivityReturnSimulatedTest {
    @Test fun onCreateAndOnNewIntentBothConnectToPersistedRoomAttempt() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val db = AppDatabase.get(context)
        val coordinator = PaymentCoordinator(db)
        val attempt = runBlocking {
            db.orderDao().insert(OrderEntity("SIMULATED-ORDER", 1234, "NAO_PAGO", "PENDING", "[]", 1, null, null, null))
            db.cashSessionDao().insert(CashSessionEntity("SIMULATED-SESSION", "Simulado", 1, openingBalanceCents = 0, status = "ABERTO"))
            coordinator.createAttempt("SIMULATED-ORDER", 1234, "DEBIT").also { coordinator.claimLaunch(it.stoneOrderId) }
        }
        val uri = "ruypos-stone://pay-response?order_id=${attempt.stoneOrderId}&code=0&amount=1234&type=DEBIT&installment_count=0&atk=SIMULATED-ATK&itk=SIMULATED-ITK&authorization_code=123456&authorization_date_time=04%2F11%2F2026"
        val controller = Robolectric.buildActivity(PosActivity::class.java, Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
        try {
            controller.create()
            await { runBlocking { db.stoneDao().byReference(attempt.stoneOrderId)!!.state == "EM_CONCILIACAO" } }
            controller.newIntent(Intent(Intent.ACTION_VIEW, Uri.parse(uri.replace("amount=1234", "amount=9999"))))
            await { runBlocking { db.stoneDao().byReference(attempt.stoneOrderId)!!.state == "INDETERMINADO" } }
            runBlocking {
                assertEquals(2, db.stoneDao().events().size)
                assertTrue(db.cashMovementDao().getBySession("SIMULATED-SESSION").isEmpty())
                assertNotNull(db.stoneDao().active())
            }
        } finally {
            controller.destroy()
            db.close()
            org.robolectric.util.ReflectionHelpers.setStaticField(AppDatabase::class.java, "INSTANCE", null)
        }
    }
    private fun await(condition: () -> Boolean) {
        repeat(200) {
            shadowOf(Looper.getMainLooper()).idle()
            if (condition()) return
            Thread.sleep(10)
        }
        fail("Simulated Activity callback did not reach Room")
    }
}
