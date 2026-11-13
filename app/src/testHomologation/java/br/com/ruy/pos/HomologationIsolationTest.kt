package br.com.ruy.pos

import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import br.com.ruy.pos.data.Repository
import br.com.ruy.pos.data.local.AppDatabase
import br.com.ruy.pos.payment.StonePaymentGateway
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class) @Config(sdk = [28])
class HomologationIsolationTest {
    @org.junit.After fun releaseSingletonForNextRobolectricSandbox() {
        val context: Context = ApplicationProvider.getApplicationContext()
        AppDatabase.get(context).close()
        org.robolectric.util.ReflectionHelpers.setStaticField(AppDatabase::class.java, "INSTANCE", null)
    }
    @Test fun separatePackageNoInternetNoSyncSeedOnceAndSimulatedLedgerOnly() = runBlocking {
        val context: Context = ApplicationProvider.getApplicationContext()
        assertTrue(BuildConfig.HOMOLOGATION)
        assertEquals("br.com.ruy.pos.homologation", context.packageName)
        assertEquals(PackageManager.PERMISSION_DENIED, context.packageManager.checkPermission("android.permission.INTERNET", context.packageName))
        assertFalse(StonePaymentGateway(context).isAvailable())
        assertTrue(SyncClient.send("https://example.invalid", "fiction", JSONObject().put("id", "fiction")).isFailure)
        Homologation.seed(context); Homologation.seed(context)
        val repo = Repository(context)
        assertEquals(2, repo.productLocalAll().size)
        assertEquals(10000, repo.getOpenCashSession()!!.openingBalanceCents)
        assertEquals(5, repo.getTables().single().number)
        val order = repo.prepareCheckout(2300, listOf("Coxinha" to 2, "Caldo de Cana" to 1), mapOf("Coxinha" to 800, "Caldo de Cana" to 700))
        repo.payments.confirmSimulated(order, 2300)
        assertEquals("SIMULADO", repo.getPayments(order).single().method)
        assertEquals("HOMOLOGATION", repo.getOrder(order)!!.syncStatus)
        assertTrue(repo.pendingOrders().isEmpty())
        val backup = br.com.ruy.pos.backup.BackupManager.backupFile(context)
        assertTrue(br.com.ruy.pos.backup.BackupManager.export(context, android.net.Uri.fromFile(backup)))
        val legacy = java.io.File(context.cacheDir, "legacy.zip")
        java.util.zip.ZipOutputStream(legacy.outputStream()).use { out ->
            java.util.zip.ZipInputStream(backup.inputStream()).use { input ->
                while (true) {
                    val entry = input.nextEntry ?: break
                    val bytes = input.readBytes()
                    if (entry.name != "manifest.json") { out.putNextEntry(java.util.zip.ZipEntry(entry.name)); out.write(bytes); out.closeEntry() }
                }
            }
        }
        assertFalse(br.com.ruy.pos.backup.BackupManager.restore(context, android.net.Uri.fromFile(legacy)))
    }
}
