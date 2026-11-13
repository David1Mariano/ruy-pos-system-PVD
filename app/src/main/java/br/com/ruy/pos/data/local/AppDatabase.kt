package br.com.ruy.pos.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import br.com.ruy.pos.data.local.Migrations.MIGRATION_1_2
import br.com.ruy.pos.data.local.Migrations.MIGRATION_2_3
import br.com.ruy.pos.data.local.Migrations.MIGRATION_3_4
import br.com.ruy.pos.data.local.Migrations.MIGRATION_4_5
import br.com.ruy.pos.data.local.Migrations.MIGRATION_5_6

@Database(entities = [ProductEntity::class, OrderEntity::class, CategoryEntity::class, ProductLocalEntity::class, TableEntity::class, TableOrderEntity::class, TableOrderItemEntity::class, PaymentEntity::class, CashSessionEntity::class, CashMovementEntity::class, CashClosingEntity::class, StoneAttemptEntity::class, StoneCallbackEventEntity::class], version = 8, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun orderDao(): OrderDao
    abstract fun categoryDao(): CategoryDao
    abstract fun productLocalDao(): ProductLocalDao
    abstract fun tableDao(): TableDao
    abstract fun tableOrderDao(): TableOrderDao
    abstract fun tableOrderItemDao(): TableOrderItemDao
    abstract fun paymentDao(): PaymentDao
    abstract fun cashSessionDao(): CashSessionDao
    abstract fun cashMovementDao(): CashMovementDao
    abstract fun cashClosingDao(): CashClosingDao
    abstract fun stoneDao(): StoneDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun get(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE?.let { return@synchronized it }
                try { br.com.ruy.pos.backup.BackupManager.applyPending(context.applicationContext) }
                catch (e: Exception) {
                    // Never open a partially replaced database if rollback itself could not finish.
                    if (java.io.File(context.filesDir, "restore_rollback/IN_PROGRESS").exists()) throw e
                    val pending = java.io.File(context.filesDir, "restore_pending")
                    if (pending.exists()) check(pending.renameTo(java.io.File(context.filesDir, "restore_rejected_${System.currentTimeMillis()}")))
                    android.util.Log.e("RuyBackup", "Restauração não aplicada; banco anterior preservado", e)
                    context.getSharedPreferences("restore_state", Context.MODE_PRIVATE).edit().putString("notice", "Restauração não aplicada; dados anteriores preservados").apply()
                }
                val inst = Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "ruy_pos_db")
                    .addMigrations(Migrations.MIGRATION_1_2, Migrations.MIGRATION_2_3, Migrations.MIGRATION_3_4, Migrations.MIGRATION_4_5, Migrations.MIGRATION_5_6, Migrations.MIGRATION_6_7, Migrations.MIGRATION_7_8)
                    .build()
                INSTANCE = inst
                inst
            }
        }
    }
}

