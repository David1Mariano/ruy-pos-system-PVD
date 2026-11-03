package br.com.ruy.pos.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ProductEntity::class, OrderEntity::class, CategoryEntity::class, ProductLocalEntity::class, TableEntity::class, TableOrderEntity::class, TableOrderItemEntity::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun orderDao(): OrderDao
    abstract fun categoryDao(): CategoryDao
    abstract fun productLocalDao(): ProductLocalDao
    abstract fun tableDao(): TableDao
    abstract fun tableOrderDao(): TableOrderDao
    abstract fun tableOrderItemDao(): TableOrderItemDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun get(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val inst = Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "ruy_pos_db")
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = inst
                inst
            }
        }
    }
}

