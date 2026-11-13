package br.com.ruy.pos.backup

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.graphics.BitmapFactory
import android.net.Uri
import br.com.ruy.pos.BuildConfig
import br.com.ruy.pos.report.ShareFiles
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.*

/** Restore is staged only. Replacement/recovery runs on cold start BEFORE Room opens. */
object BackupManager {
    private const val DB = "ruy_pos_db"
    private val imageDirs = listOf("product_images", "product_photos")
    private const val LIMIT = 256L * 1024 * 1024
    private val databaseFiles = listOf(DB, "$DB-wal", "$DB-shm")
    private fun digest(f: File): String {
        val hash = MessageDigest.getInstance("SHA-256")
        f.inputStream().use { input -> val b = ByteArray(8192); while (true) { val n = input.read(b); if (n < 0) break; hash.update(b, 0, n) } }
        return hash.digest().joinToString("") { "%02x".format(it) }
    }
    private fun syncWrite(file: File, value: String) {
        FileOutputStream(file).use { it.write(value.toByteArray(Charsets.UTF_8)); it.fd.sync() }
    }
    private fun payloadFiles(dir: File) = dir.walkTopDown().filter { it.isFile && it.name != "manifest.json" && !it.name.endsWith("-shm") }.toList()
    private fun manifest(context: Context, dir: File): JSONObject {
        val hashes = JSONObject()
        payloadFiles(dir).forEach { hashes.put(it.relativeTo(dir).invariantSeparatorsPath, digest(it)) }
        return JSONObject().put("format", 2).put("package", context.packageName).put("homologation", BuildConfig.HOMOLOGATION)
            .put("schema", 8).put("files", hashes)
    }
    private fun validate(context: Context, dir: File) {
        val meta = JSONObject(File(dir, "manifest.json").readText())
        require(meta.getInt("format") == 2 && meta.getInt("schema") == 8)
        require(meta.getString("package") == context.packageName && meta.getBoolean("homologation") == BuildConfig.HOMOLOGATION) {
            "Backup de outro aplicativo/ambiente recusado"
        }
        val hashes = meta.getJSONObject("files")
        val names = hashes.keys().asSequence().toSet()
        require(DB in names && names == payloadFiles(dir).map { it.relativeTo(dir).invariantSeparatorsPath }.toSet()) { "Arquivos divergentes do manifesto" }
        names.forEach { require(digest(File(dir, it)) == hashes.getString(it)) { "Checksum divergente: $it" } }
        SQLiteDatabase.openDatabase(File(dir, DB).path, null, SQLiteDatabase.OPEN_READONLY).use { sql ->
            sql.rawQuery("PRAGMA integrity_check", null).use { require(it.moveToFirst() && it.getString(0) == "ok" && !it.moveToNext()) }
            sql.rawQuery("PRAGMA foreign_key_check", null).use { require(!it.moveToFirst()) }
            require(sql.version == 8)
            sql.rawQuery("SELECT identity_hash FROM room_master_table WHERE id=42", null).use {
                require(it.moveToFirst() && it.getString(0) == "b1484f4783fcea3e552e5d7447672244")
            }
            for (table in listOf("orders", "products", "categories", "products_local", "tables", "table_orders", "table_order_items",
                "payments", "cash_sessions", "cash_movements", "cash_closings", "stone_attempts", "stone_callback_events")) {
                sql.rawQuery("SELECT * FROM $table", null).use { while (it.moveToNext()) { /* force SQLite page reads */ } }
            }
            sql.rawQuery("SELECT imagePath FROM products_local WHERE imagePath IS NOT NULL", null).use { rows ->
                while (rows.moveToNext()) {
                    val original = File(rows.getString(0))
                    val folder = original.parentFile?.name
                    require(folder in imageDirs)
                    require(File(dir, "$folder/${original.name}").isFile) { "Foto referenciada ausente" }
                }
            }
        }
        for (folder in imageDirs) File(dir, folder).listFiles()?.forEach { photo ->
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(photo.path, options)
            require(options.outWidth > 0 && options.outHeight > 0) { "Imagem inválida" }
        }
    }

    @Synchronized fun export(context: Context, target: Uri): Boolean {
        val staging = File(context.cacheDir, "e-${UUID.randomUUID().toString().take(12)}").apply { check(mkdirs()) }
        return try {
            val live = context.getDatabasePath(DB)
            require(live.isFile)
            SQLiteDatabase.openDatabase(live.path, null, SQLiteDatabase.OPEN_READWRITE or SQLiteDatabase.ENABLE_WRITE_AHEAD_LOGGING).use { sql ->
                // Freeze writers while copying DB + WAL. Sidecars live beside DB, never filesDir.
                sql.execSQL("BEGIN IMMEDIATE")
                try {
                    listOf(DB, "$DB-wal").forEach { name -> File(live.parentFile, name).takeIf { it.isFile }?.copyTo(File(staging, name)) }
                    imageDirs.forEach { folder -> File(context.filesDir, folder).takeIf { it.exists() }?.let { check(it.copyRecursively(File(staging, folder))) } }
                } finally { sql.execSQL("ROLLBACK") }
            }
            // Consolidate the isolated snapshot, never checkpoint/modify the operational database.
            SQLiteDatabase.openDatabase(File(staging, DB).path, null, SQLiteDatabase.OPEN_READWRITE).use { sql ->
                sql.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", null).use { while (it.moveToNext()) {} }
                sql.rawQuery("PRAGMA journal_mode=DELETE", null).use { require(it.moveToFirst() && it.getString(0).equals("delete", true)) }
            }
            File(staging, "$DB-wal").delete(); File(staging, "$DB-shm").delete()
            syncWrite(File(staging, "manifest.json"), manifest(context, staging).toString())
            validate(context, staging)
            File(staging, "$DB-shm").delete()
            val output = context.contentResolver.openOutputStream(target) ?: error("Destino indisponível")
            ZipOutputStream(output).use { zip ->
                staging.walkTopDown().filter { it.isFile }.forEach { f ->
                    zip.putNextEntry(ZipEntry(f.relativeTo(staging).invariantSeparatorsPath))
                    f.inputStream().use { it.copyTo(zip) }; zip.closeEntry()
                }
            }
            true
        } catch (e: Exception) { android.util.Log.e("RuyBackup", "Falha na exportação", e); false } finally { staging.deleteRecursively() }
    }

    @Synchronized fun restore(context: Context, source: Uri): Boolean {
        val staging = File(context.filesDir, "r-${UUID.randomUUID().toString().take(12)}").apply { check(mkdirs()) }
        val pending = File(context.filesDir, "restore_pending")
        return try {
            check(!pending.exists()) { "Restauração já preparada; reinicie" }
            val seen = mutableSetOf<String>()
            var bytes = 0L
            ZipInputStream(context.contentResolver.openInputStream(source) ?: error("Arquivo ausente")).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    val name = entry.name
                    require(!entry.isDirectory && seen.add(name) && seen.size <= 2000)
                    require(name in listOf(DB, "$DB-wal", "$DB-shm", "manifest.json") || name.matches(Regex("(product_images|product_photos)/[A-Za-z0-9._-]+")))
                    val file = File(staging, name)
                    require(file.canonicalPath.startsWith(staging.canonicalPath + File.separator))
                    file.parentFile?.mkdirs()
                    FileOutputStream(file).use { out ->
                        val buffer = ByteArray(8192)
                        while (true) {
                            val n = zip.read(buffer); if (n < 0) break
                            bytes += n; require(bytes <= LIMIT)
                            out.write(buffer, 0, n)
                        }
                        out.fd.sync()
                    }
                    zip.closeEntry()
                }
            }
            if (!File(staging, "manifest.json").exists()) {
                // Historic backups have no environment provenance: NEVER import them into homologation.
                check(!BuildConfig.HOMOLOGATION) { "Backup legado não permitido na homologação" }
                require(File(staging, DB).isFile)
                File(staging, "$DB-shm").delete()
                // Migrate/validate a COPY with the same non-destructive Room migrations as normal startup.
                val copy = androidx.room.Room.databaseBuilder(context, br.com.ruy.pos.data.local.AppDatabase::class.java,
                    File(staging, DB).absolutePath).addMigrations(
                        br.com.ruy.pos.data.local.Migrations.MIGRATION_1_2, br.com.ruy.pos.data.local.Migrations.MIGRATION_2_3,
                        br.com.ruy.pos.data.local.Migrations.MIGRATION_3_4, br.com.ruy.pos.data.local.Migrations.MIGRATION_4_5,
                        br.com.ruy.pos.data.local.Migrations.MIGRATION_5_6, br.com.ruy.pos.data.local.Migrations.MIGRATION_6_7,
                        br.com.ruy.pos.data.local.Migrations.MIGRATION_7_8).allowMainThreadQueries().build()
                try { require(copy.openHelper.writableDatabase.version == 8) } finally { copy.close() }
                syncWrite(File(staging, "manifest.json"), manifest(context, staging).toString())
            }
            validate(context, staging)
            File(staging, "$DB-shm").delete()
            // Normalize private photo paths for this installation AFTER complete verification.
            SQLiteDatabase.openDatabase(File(staging, DB).path, null, SQLiteDatabase.OPEN_READWRITE).use { sql ->
                sql.rawQuery("SELECT id,imagePath FROM products_local WHERE imagePath IS NOT NULL", null).use { rows ->
                    while (rows.moveToNext()) {
                        val old = File(rows.getString(1))
                        sql.execSQL("UPDATE products_local SET imagePath=? WHERE id=?", arrayOf(File(context.filesDir, "${old.parentFile!!.name}/${old.name}").absolutePath, rows.getString(0)))
                    }
                }
                sql.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", null).use { while(it.moveToNext()) {} }
            }
            File(staging, "$DB-shm").delete()
            syncWrite(File(staging, "manifest.json"), manifest(context, staging).toString())
            check(staging.renameTo(pending))
            true
        } catch (e: Exception) { android.util.Log.e("RuyBackup", "Arquivo de restauração rejeitado", e); false } finally { staging.deleteRecursively() }
    }

    /** Called only before Room opens. Rollback copy survives interruption of any replacement. */
    @Synchronized fun applyPending(context: Context, fault: (String) -> Unit = {}) {
        val pending = File(context.filesDir, "restore_pending")
        val rollback = File(context.filesDir, "restore_rollback")
        val live = context.getDatabasePath(DB)
        fun replace(from: File) {
            live.parentFile!!.mkdirs()
            databaseFiles.forEach { name ->
                val src = File(from, name); val dst = File(live.parentFile, name)
                if (src.exists()) src.copyTo(dst, overwrite = true) else check(!dst.exists() || dst.delete())
            }
            imageDirs.forEach { folder ->
                val dst = File(context.filesDir, folder)
                check(!dst.exists() || dst.deleteRecursively())
                File(from, folder).takeIf { it.exists() }?.let { check(it.copyRecursively(dst)) }
            }
        }
        if (File(rollback, "IN_PROGRESS").exists()) {
            replace(rollback)
            check(pending.deleteRecursively()); check(rollback.deleteRecursively())
            return
        }
        if (File(rollback, "COMMITTED").exists()) {
            if (pending.deleteRecursively()) rollback.deleteRecursively()
            return
        }
        if (!pending.exists()) return
        validate(context, pending)
        check(!rollback.exists() || rollback.deleteRecursively()); check(rollback.mkdirs())
        databaseFiles.forEach { name -> File(live.parentFile, name).takeIf { it.exists() }?.copyTo(File(rollback, name)) }
        imageDirs.forEach { folder -> File(context.filesDir, folder).takeIf { it.exists() }?.let { check(it.copyRecursively(File(rollback, folder))) } }
        syncWrite(File(rollback, "IN_PROGRESS"), "rollback prepared")
        try {
            fault("beforeReplace")
            replace(pending)
            fault("afterReplace")
            SQLiteDatabase.openDatabase(live.path, null, SQLiteDatabase.OPEN_READONLY).use { sql ->
                sql.rawQuery("PRAGMA integrity_check", null).use { require(it.moveToFirst() && it.getString(0) == "ok") }
            }
            check(File(rollback, "IN_PROGRESS").renameTo(File(rollback, "COMMITTED")))
        } catch (e: Exception) {
            replace(rollback)
            check(pending.deleteRecursively()); check(rollback.deleteRecursively())
            throw e
        }
        // A cleanup failure AFTER commit must never invoke rollback or reapply an old snapshot later.
        // Keep COMMITTED until pending removal is complete; next startup only retries cleanup.
        if (pending.deleteRecursively()) rollback.deleteRecursively()
    }

    fun backupFile(context: Context): File = ShareFiles.exportFile(context, "ruy_pos_backup.zip")
}
