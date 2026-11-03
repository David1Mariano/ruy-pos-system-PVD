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

@Database(entities = [ProductEntity::class, OrderEntity::class, CategoryEntity::class, ProductLocalEntity::class, TableEntity::class, TableOrderEntity::class, TableOrderItemEntity::class, PaymentEntity::class], version = 6, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun orderDao(): OrderDao
    abstract fun categoryDao(): CategoryDao
    abstract fun productLocalDao(): ProductLocalDao
    abstract fun tableDao(): TableDao
    abstract fun tableOrderDao(): TableOrderDao
    abstract fun tableOrderItemDao(): TableOrderItemDao
    abstract fun paymentDao(): PaymentDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun get(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val inst = Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "ruy_pos_db")
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .build()
                INSTANCE = inst
                inst
            }
        }
    }
}

