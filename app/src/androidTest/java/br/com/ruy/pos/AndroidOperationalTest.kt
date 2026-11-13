package br.com.ruy.pos

import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.ruy.pos.backup.BackupManager
import br.com.ruy.pos.data.Repository
import br.com.ruy.pos.data.local.AppDatabase
import br.com.ruy.pos.report.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File

/** Executable ONLY against applicationId .homologation; fixture DB/files are additionally isolated. */
@RunWith(AndroidJUnit4::class)
class AndroidOperationalTest {
    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repo: Repository
    @Before fun setup() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        check(BuildConfig.HOMOLOGATION && base.packageName == "br.com.ruy.pos.homologation")
        context = object : ContextWrapper(base) {
            override fun getDatabasePath(name: String) = base.getDatabasePath("fixture_$name")
            override fun getFilesDir() = File(base.filesDir, "instrumented-fixture").apply { mkdirs() }
        }
        context.deleteDatabase("fixture_ruy_pos_db")
        open()
    }
    private fun open() {
        db = Room.databaseBuilder(context, AppDatabase::class.java, "ruy_pos_db").allowMainThreadQueries().build()
        repo = Repository(db)
    }
    @After fun cleanup() {
        db.close()
        context.getDatabasePath("ruy_pos_db").delete()
        context.filesDir.deleteRecursively()
    }
    @Test fun offlineSaleTableCashPdfSharingBackupRestoreAndDatabaseReopen() = runBlocking {
        val photo = File(context.filesDir, "product_images/fixture.png").apply { parentFile!!.mkdirs() }
        Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888).let { b -> photo.outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }; b.recycle() }
        repo.saveCategory("fixture-salgados", "Salgados")
        repo.saveProductLocal("fixture-coxinha", "fixture-salgados", "Coxinha", 800, imagePath = photo.path)
        repo.saveProductLocal("fixture-caldo", null, "Caldo de Cana", 700)
        repo.ensureTables(5)
        val session = repo.openCashSession("Operador fictício", 10000)
        val comanda = repo.openTable("T05")
        repo.addItemToComanda(comanda, "Coxinha", 800, 2)
        repo.addItemToComanda(comanda, "Caldo de Cana", 700, 1)
        assertEquals(2300, repo.getComandaTotal(comanda))
        val order = repo.prepareCheckout(2300, emptyList(), emptyMap(), comanda)
        assertEquals(order, repo.prepareCheckout(2300, emptyList(), emptyMap(), comanda))
        assertFalse(repo.closeComandaIfPaid(comanda))
        repo.payments.confirmCash(order, 1600, 1600)
        assertFalse(repo.closeComandaIfPaid(comanda)); assertEquals("OCUPADA", db.tableDao().byId("T05")!!.status)
        repo.payments.confirmCash(order, 700, 700)
        assertTrue(repo.closeComandaIfPaid(comanda)); assertEquals("LIVRE", db.tableDao().byId("T05")!!.status)
        repo.addCashMovement(session, "SANGRIA", 2000, "Sangria fictícia")
        val closing = repo.payments.closeCash(session, 10300)
        assertEquals(10300, closing.expectedCashCents); assertEquals(0, closing.differenceCents)
        val movements = repo.getCashMovements(session); val payments = repo.getAllPayments(); val orders = repo.allOrders()
        val pdf = PdfReport.generateCashClosing(context, closing, movements, payments, closing.operator)
        PdfRenderer(ParcelFileDescriptor.open(pdf, ParcelFileDescriptor.MODE_READ_ONLY)).use { renderer ->
            assertTrue(renderer.pageCount >= 1)
            renderer.openPage(0).use { assertEquals(595, it.width); assertEquals(842, it.height) }
        }
        val share = ShareFiles.intent(context, pdf, "application/pdf")
        val uri = share.getParcelableExtra<Uri>(android.content.Intent.EXTRA_STREAM)!!
        assertEquals("content", uri.scheme)
        assertTrue(share.flags and android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        context.contentResolver.openInputStream(uri)!!.use { assertTrue(it.readBytes().size > 100) }
        val backup = BackupManager.backupFile(context)
        assertTrue(BackupManager.export(context, Uri.fromFile(backup)))
        repo.saveProductLocal("later", null, "Posterior", 100)
        assertTrue(BackupManager.restore(context, Uri.fromFile(backup)))
        db.close(); BackupManager.applyPending(context); open()
        assertEquals(orders, repo.allOrders()); assertEquals(payments, repo.getAllPayments())
        assertEquals(movements, repo.getCashMovements(session)); assertEquals(2, repo.productLocalAll().size)
        assertTrue(photo.exists()); assertEquals(1, repo.getCashClosings().size)
    }
    @Test fun permissionsAndSyncCannotReachProduction() {
        assertEquals(PackageManager.PERMISSION_DENIED, context.packageManager.checkPermission("android.permission.INTERNET", context.packageName))
        assertTrue(SyncClient.send("https://example.invalid", "fiction", org.json.JSONObject().put("id", "fiction")).isFailure)
        assertFalse(br.com.ruy.pos.payment.StonePaymentGateway(context).isAvailable())
    }
}
