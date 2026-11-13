package br.com.ruy.pos.backup

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import br.com.ruy.pos.data.Repository
import br.com.ruy.pos.data.local.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.zip.*

/** Exercises real backup implementation against isolated SQLite files and PNG image bytes. */
@RunWith(RobolectricTestRunner::class) @Config(sdk = [28])
class BackupManagerSimulatedTest {
    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repo: Repository
    private lateinit var archive: File
    private lateinit var photo: File
    @Before fun setup() = runBlocking {
        org.robolectric.shadows.ShadowLog.stream = System.err
        context = ApplicationProvider.getApplicationContext()
        open()
        photo = File(context.filesDir, "product_images/ficticio.png").apply { parentFile!!.mkdirs() }
        val bitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        photo.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }; bitmap.recycle()
        repo.saveCategory("FICTICIO", "Salgados")
        repo.saveProductLocal("FICTICIO-COXINHA", "FICTICIO", "Coxinha", 800, imagePath = photo.path)
        repo.openCashSession("Fictício", 10000)
        val order = repo.prepareCheckout(2300, listOf("Coxinha" to 2, "Caldo" to 1), mapOf("Coxinha" to 800, "Caldo" to 700))
        repo.payments.confirmCash(order, 2300, 2300)
        archive = BackupManager.backupFile(context)
    }
    private fun open() {
        db = Room.databaseBuilder(context, AppDatabase::class.java, "ruy_pos_db").allowMainThreadQueries().build()
        repo = Repository(db)
    }
    @After fun cleanup() {
        db.close(); context.deleteDatabase("ruy_pos_db")
        listOf("product_images", "product_photos", "restore_pending", "restore_rollback").forEach { File(context.filesDir, it).deleteRecursively() }
    }
    private fun export() = assertTrue("Complete SQLite/image export must succeed", BackupManager.export(context, Uri.fromFile(archive)))
    private fun editedArchive(skip: String? = null, change: ((String, ByteArray) -> ByteArray)? = null): File {
        val target = File(context.cacheDir, "edited.zip")
        ZipOutputStream(target.outputStream()).use { output ->
            ZipInputStream(archive.inputStream()).use { input ->
                while (true) {
                    val e = input.nextEntry ?: break
                    val bytes = input.readBytes()
                    if (e.name != skip) {
                        output.putNextEntry(ZipEntry(e.name)); output.write(change?.invoke(e.name, bytes) ?: bytes); output.closeEntry()
                    }
                }
            }
        }
        return target
    }
    @Test fun backupRestoreReopenComparesOrdersPaymentsLedgerProductsAndImages() = runBlocking {
        val orders = repo.allOrders(); val payments = repo.getAllPayments(); val products = repo.productLocalAll()
        val session = repo.getOpenCashSession()!!; val movements = repo.getCashMovements(session.sessionId)
        val image = photo.readBytes()
        export()
        repo.saveProductLocal("EXTRA", null, "Extra posterior", 100)
        assertTrue(BackupManager.restore(context, Uri.fromFile(archive)))
        // Preparing never replaces live Room data.
        assertEquals(2, repo.productLocalAll().size)
        db.close(); BackupManager.applyPending(context); open()
        assertEquals(orders, repo.allOrders()); assertEquals(payments, repo.getAllPayments())
        assertEquals(products, repo.productLocalAll()); assertEquals(movements, repo.getCashMovements(session.sessionId))
        assertArrayEquals(image, photo.readBytes())
        assertFalse(File(context.filesDir, "restore_rollback").exists())
    }
    @Test fun corruptZipMissingDatabaseMissingPhotoAndChecksumMismatchLeaveLiveUntouched() = runBlocking {
        export(); val before = repo.allOrders()
        val bad = File(context.cacheDir, "broken.zip").apply { writeText("not a zip") }
        assertFalse(BackupManager.restore(context, Uri.fromFile(bad)))
        for (missing in listOf("ruy_pos_db", "product_images/ficticio.png"))
            assertFalse(BackupManager.restore(context, Uri.fromFile(editedArchive(skip = missing))))
        assertFalse(BackupManager.restore(context, Uri.fromFile(editedArchive(change = { name, bytes ->
            if (name == "ruy_pos_db") bytes.also { it[it.lastIndex] = (it.last().toInt() xor 1).toByte() } else bytes
        }))))
        assertEquals(before, repo.allOrders()); assertTrue(photo.isFile)
        assertFalse(File(context.filesDir, "restore_pending").exists())
    }
    @Test fun validHeaderAloneCannotPassDatabaseValidation() = runBlocking {
        db.close()
        val live = context.getDatabasePath("ruy_pos_db")
        val bytes = live.readBytes(); bytes.fill(0, 100, bytes.size); live.writeBytes(bytes)
        assertFalse(BackupManager.export(context, Uri.fromFile(archive)))
    }
    @Test fun legacyBackupWithoutManifestStillRequiresFullSqliteAndImageValidation() = runBlocking {
        export()
        if (br.com.ruy.pos.BuildConfig.HOMOLOGATION) {
            assertFalse(BackupManager.restore(context, Uri.fromFile(editedArchive(skip = "manifest.json"))))
            return@runBlocking
        }
        assertTrue(BackupManager.restore(context, Uri.fromFile(editedArchive(skip = "manifest.json"))))
        db.close(); BackupManager.applyPending(context); open()
        assertEquals(1, repo.allOrders().size); assertEquals("Coxinha", repo.productLocalAll().single().name)
        assertTrue(photo.exists())
    }
    @Test fun pathTraversalAndOtherEnvironmentAreRejected() = runBlocking {
        export()
        val bad = File(context.cacheDir, "traversal.zip")
        ZipOutputStream(bad.outputStream()).use { it.putNextEntry(ZipEntry("../escaped")); it.write(byteArrayOf(1)); it.closeEntry() }
        assertFalse(BackupManager.restore(context, Uri.fromFile(bad)))
        assertFalse(File(context.filesDir, "escaped").exists())
        val other = editedArchive(change = { name, bytes ->
            if (name == "manifest.json") String(bytes).replace(context.packageName, "other.package").toByteArray() else bytes
        })
        assertFalse(BackupManager.restore(context, Uri.fromFile(other)))
    }
    @Test fun interruptedReplacementRollsBackDatabaseAndImagesOnNextStartup() = runBlocking {
        export()
        repo.saveProductLocal("LATER", null, "Preservar após interrupção", 100)
        val before = repo.productLocalAll(); val image = photo.readBytes()
        assertTrue(BackupManager.restore(context, Uri.fromFile(archive)))
        db.close()
        class ProcessInterrupted : Error()
        try { BackupManager.applyPending(context) { if (it == "afterReplace") throw ProcessInterrupted() }; fail("Must interrupt") }
        catch (_: ProcessInterrupted) { }
        assertTrue(File(context.filesDir, "restore_rollback/IN_PROGRESS").exists())
        BackupManager.applyPending(context); open()
        assertEquals(before, repo.productLocalAll()); assertArrayEquals(image, photo.readBytes())
        assertFalse(File(context.filesDir, "restore_pending").exists())
    }
    @Test fun recoverableExceptionAlsoRollsBackAndDoesNotReportSuccess() = runBlocking {
        export(); val before = repo.allOrders()
        assertTrue(BackupManager.restore(context, Uri.fromFile(archive))); db.close()
        assertThrows(IllegalStateException::class.java) { BackupManager.applyPending(context) { error("Fictitious IO failure") } }
        open(); assertEquals(before, repo.allOrders())
    }
}
