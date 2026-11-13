package br.com.ruy.pos.payment

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import br.com.ruy.pos.data.local.AppDatabase
import br.com.ruy.pos.data.local.Migrations
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

/** SIMULATED historical SQLite v7 -> Room v8, including Room's complete schema validation. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class StoneMigrationSimulatedTest {
    @Test fun migratesWithoutDestroyingOrdersPaymentsCashOrOccupiedTables() = runBlocking {
        val context: Context = ApplicationProvider.getApplicationContext()
        val name = "m.db" // Robolectric uses a per-test directory; keep Windows SQLite sidecars below MAX_PATH.
        val helper = FrameworkSQLiteOpenHelperFactory().create(SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(name).callback(object : SupportSQLiteOpenHelper.Callback(7) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    val sql = javaClass.classLoader!!.getResourceAsStream("room-v7.sql")!!.bufferedReader().use { it.readText() }
                    sql.lineSequence().filterNot { it.startsWith("--") }.joinToString("\n").split(';')
                        .map { it.trim() }.filter { it.isNotEmpty() }.forEach { db.execSQL(it) }
                }
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) { error("Unexpected fixture upgrade") }
            }).build())
        helper.writableDatabase.apply {
            execSQL("INSERT INTO orders VALUES ('SIMULATED-UUID',1234,'NAO_PAGO','PENDING','[]',1,NULL,NULL,NULL)")
            execSQL("INSERT INTO payments VALUES ('SIMULATED-PAYMENT','SIMULATED-UUID',1234,'PIX','AGUARDANDO',NULL,NULL,1,NULL,1)")
            execSQL("INSERT INTO tables VALUES ('T01',1,'OCUPADA',1,NULL)")
            execSQL("INSERT INTO table_orders VALUES ('SIMULATED-COMANDA','T01','ABERTA',1234,1,NULL)")
            execSQL("INSERT INTO cash_sessions VALUES ('SIMULATED-SESSION','Operador',1,NULL,1000,'ABERTO')")
            execSQL("INSERT INTO cash_movements VALUES ('SIMULATED-MOVEMENT','SIMULATED-SESSION','SUPRIMENTO',500,NULL,NULL,1)")
        }
        helper.close()
        val migrated = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(Migrations.MIGRATION_7_8).allowMainThreadQueries().build()
        try {
            assertEquals(8, migrated.openHelper.writableDatabase.version)
            assertEquals(1234, migrated.orderDao().byId("SIMULATED-UUID")!!.totalCents)
            assertNull(migrated.orderDao().byId("SIMULATED-UUID")!!.comandaId) // Never guess historical links.
            assertEquals("AGUARDANDO", migrated.paymentDao().byId("SIMULATED-PAYMENT")!!.status)
            assertEquals("OCUPADA", migrated.tableDao().byId("T01")!!.status)
            assertEquals("ABERTA", migrated.tableOrderDao().byId("SIMULATED-COMANDA")!!.status)
            assertEquals(500, migrated.cashMovementDao().getBySession("SIMULATED-SESSION").single().amountCents)
            assertNull(migrated.stoneDao().active())
        } finally { migrated.close(); context.deleteDatabase(name) }
    }
}
