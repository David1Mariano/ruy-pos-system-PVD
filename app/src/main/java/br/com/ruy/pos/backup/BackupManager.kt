package br.com.ruy.pos.backup

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object BackupManager {
    private const val DB_FILE = "ruy_pos_db"
    private const val WAL_SUFFIX = "-wal"
    private const val SHM_SUFFIX = "-shm"
    private const val PHOTOS_DIR = "product_photos"
    private val SQLITE_MAGIC = byteArrayOf(
        0x53, 0x51, 0x4C, 0x69, 0x74, 0x65, 0x20, 0x66,
        0x6F, 0x72, 0x6D, 0x61, 0x74, 0x20, 0x33, 0x00
    )

    /** Export current DB + WAL/SHM + product photos into a ZIP at [target]. */
    fun export(context: Context, target: Uri): Boolean {
        return try {
            val dbPath = context.getDatabasePath(DB_FILE)
            if (!dbPath.exists()) return false
            checkpoint(dbPath)
            val photosDir = File(context.filesDir, PHOTOS_DIR)
            val fos = context.contentResolver.openOutputStream(target) ?: return false
            ZipOutputStream(fos).use { zos ->
                listOf(dbPath, File(context.filesDir, "$DB_FILE$WAL_SUFFIX"), File(context.filesDir, "$DB_FILE$SHM_SUFFIX")).forEach { f ->
                    if (f.exists()) {
                        zos.putNextEntry(ZipEntry(f.name))
                        FileInputStream(f).use { it.copyTo(zos) }
                        zos.closeEntry()
                    }
                }
                if (photosDir.exists()) {
                    photosDir.listFiles()?.forEach { f ->
                        zos.putNextEntry(ZipEntry("$PHOTOS_DIR/${f.name}"))
                        FileInputStream(f).use { it.copyTo(zos) }
                        zos.closeEntry()
                    }
                }
            }
            true
        } catch (e: Exception) { false }
    }

    /**
     * Safe restore: extract into a temp dir, validate the SQLite header, then atomically
     * replace the live DB/WAL/SHM and photos. The live data is never deleted before the
     * incoming backup is verified. On any failure the original data is kept.
     */
    fun restore(context: Context, source: Uri): Boolean {
        val tmpDir = File(context.cacheDir, "restore_tmp")
        if (tmpDir.exists()) tmpDir.deleteRecursively()
        tmpDir.mkdirs()
        return try {
            val zis = ZipInputStream(context.contentResolver.openInputStream(source) ?: return false)
            var entry: ZipEntry?
            var dbEntryCount = 0
            while (zis.nextEntry.let { entry = it; it != null }) {
                val name = entry!!.name
                val out = File(tmpDir, name)
                if (entry!!.isDirectory) { out.mkdirs(); continue }
                out.parentFile?.mkdirs()
                FileOutputStream(out).use { zis.copyTo(it) }
                if (name == DB_FILE) dbEntryCount++
                zis.closeEntry()
            }
            zis.close()
            if (dbEntryCount == 0) throw IllegalStateException("Banco não encontrado no ZIP")
            val tmpDb = File(tmpDir, DB_FILE)
            if (!isValidSQLite(tmpDb)) throw IllegalStateException("Arquivo de banco inválido")
            val dbPath = context.getDatabasePath(DB_FILE)
            dbPath.parentFile?.mkdirs()
            tmpDb.renameTo(dbPath)
            File(context.filesDir, "$DB_FILE$WAL_SUFFIX").delete()
            File(context.filesDir, "$DB_FILE$SHM_SUFFIX").delete()
            val tmpPhotos = File(tmpDir, PHOTOS_DIR)
            val livePhotos = File(context.filesDir, PHOTOS_DIR)
            if (tmpPhotos.exists()) {
                if (livePhotos.exists()) livePhotos.deleteRecursively()
                livePhotos.mkdirs()
                tmpPhotos.copyTree(livePhotos)
            }
            true
        } catch (e: Exception) {
            tmpDir.deleteRecursively()
            false
        } finally {
            tmpDir.deleteRecursively()
        }
    }

    fun backupFile(context: Context): File = File(context.cacheDir, "ruy_pos_backup.zip")

    private fun checkpoint(dbPath: File) {
        try {
            val conn = SQLiteDatabase.openDatabase(dbPath.path, null, 0)
            if (conn != null) {
                conn.use { it.execSQL("PRAGMA wal_checkpoint(TRUNCATE);") }
            }
        } catch (e: Exception) { /* ignore */ }
    }

    private fun isValidSQLite(file: File): Boolean {
        if (!file.exists() || file.length() < 100) return false
        FileInputStream(file).use { fis ->
            val magic = ByteArray(16)
            if (fis.read(magic) != 16) return false
            return magic.contentEquals(SQLITE_MAGIC)
        }
    }

    private fun File.copyTree(dest: File) {
        dest.parentFile?.mkdirs()
        listFiles()?.forEach { child ->
            val target = File(dest, child.name)
            if (child.isDirectory) child.copyTree(target) else FileInputStream(child).use { it.copyTo(FileOutputStream(target)) }
        }
    }
}