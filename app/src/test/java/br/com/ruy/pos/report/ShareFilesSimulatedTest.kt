package br.com.ruy.pos.report

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class) @Config(sdk = [28])
class ShareFilesSimulatedTest {
    @org.junit.Before fun resetProviderCacheForRobolectricTemporaryDirectories() {
        org.junit.Assume.assumeTrue("AndroidX FileProvider path containment requires Android/Linux separators; verify on the native instrumented suite on Windows hosts", File.separatorChar == '/')
        // Android keeps one files/cache directory per process; Robolectric changes it per test method.
        org.robolectric.util.ReflectionHelpers.getStaticField<MutableMap<String, Any>>(androidx.core.content.FileProvider::class.java, "sCache").clear()
    }
    @Test fun exportedFileUsesReadableContentUriAndReadGrant() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val file = ShareFiles.exportFile(context, "fixture.txt").apply { writeText("FICTICIO") }
        val intent = ShareFiles.intent(context, file, "text/plain")
        val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)!!
        assertEquals("content", uri.scheme)
        assertEquals("${context.packageName}.files", uri.authority)
        assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        assertEquals(uri, intent.clipData!!.getItemAt(0).uri)
        assertEquals("FICTICIO", context.contentResolver.openInputStream(uri)!!.bufferedReader().use { it.readText() })
    }
    @Test fun providerCannotExposePrivateDatabaseOrOtherCachePaths() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val private = File(context.filesDir, "fixture-private").apply { writeText("FICTICIO") }
        assertThrows(IllegalArgumentException::class.java) { ShareFiles.intent(context, private, "text/plain") }
        val other = File(context.cacheDir, "fixture-private").apply { writeText("FICTICIO") }
        assertThrows(IllegalArgumentException::class.java) { ShareFiles.intent(context, other, "text/plain") }
    }
}
