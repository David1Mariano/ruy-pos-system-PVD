package br.com.ruy.pos.backup

import android.content.Context
import android.net.Uri
import android.util.Base64
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object BackupManager {
    private const val BACKUP_FILE = "ruy_pos_backup.zip"
    private const val DB_FILE = "ruy_pos_db"

    fun export(context: Context, target: Uri): Boolean {
        return try {
            val dbPath = context.getDatabasePath(DB_FILE)
            if (!dbPath.exists()) return false
            val photosDir = File(context.filesDir, "product_photos")
            val fos = context.contentResolver.openOutputStream(target) ?: return false
            ZipOutputStream(fos).use { zos ->
                // Room/WAL files
                listOf(dbPath, File(context.filesDir, "$DB_FILE-wal"), File(context.filesDir, "$DB_FILE-shm")).forEach { f ->
                    if (f.exists()) {
                        zos.putNextEntry(ZipEntry(f.name))
                        FileInputStream(f).use { it.copyTo(zos) }
                        zos.closeEntry()
                    }
                }
                // Product photos
                if (photosDir.exists()) {
                    photosDir.listFiles()?.forEach { f ->
                        zos.putNextEntry(ZipEntry("product_photos/${f.name}"))
                        FileInputStream(f).use { it.copyTo(zos) }
                        zos.closeEntry()
                    }
                }
            }
            true
        } catch (e: Exception) { false }
    }

    fun restore(context: Context, source: Uri): Boolean {
        return try {
            val dbPath = context.getDatabasePath(DB_FILE)
            if (dbPath.exists()) dbPath.delete()
            File(context.filesDir, "$DB_FILE-wal").delete()
            File(context.filesDir, "$DB_FILE-shm").delete()
            val photosDir = File(context.filesDir, "product_photos")
            if (photosDir.exists()) photosDir.deleteRecursively()
            photosDir.mkdirs()
            val zis = java.util.zip.ZipInputStream(context.contentResolver.openInputStream(source) ?: return false)
            var entry: ZipEntry?
            while (zis.nextEntry.let { entry = it; it != null }) {
                val name = entry!!.name
                val out = if (name.startsWith("product_photos/")) File(photosDir, name.removePrefix("product_photos/")) else File(context.filesDir, name)
                if (entry!!.isDirectory) { out.mkdirs(); continue }
                out.parentFile?.mkdirs()
                FileOutputStream(out).use { zis.copyTo(it) }
                zis.closeEntry()
            }
            zis.close()
            true
        } catch (e: Exception) { false }
    }

    fun backupFile(context: Context): File = File(context.cacheDir, BACKUP_FILE)
}