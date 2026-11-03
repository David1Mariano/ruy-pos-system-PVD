package br.com.ruy.pos.data.local

import androidx.room.*

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE active = 1 ORDER BY name")
    suspend fun getActive(): List<ProductEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(list: List<ProductEntity>)
    @Query("DELETE FROM products")
    suspend fun clear()
}

@Dao
interface OrderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(order: OrderEntity)
    @Query("SELECT * FROM orders WHERE syncStatus = 'PENDING' ORDER BY createdAt")
    suspend fun pending(): List<OrderEntity>
    @Query("UPDATE orders SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String)
    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    suspend fun all(): List<OrderEntity>
}

@Dao
interface SyncQueueDao {
    @Insert
    suspend fun enqueue(item: SyncQueueEntity): Long
    @Query("SELECT * FROM sync_queue ORDER BY createdAt ASC LIMIT :limit")
    suspend fun nextBatch(limit: Int): List<SyncQueueEntity>
    @Query("DELETE FROM sync_queue WHERE rowId = :rowId")
    suspend fun remove(rowId: Long)
    @Query("UPDATE sync_queue SET attempts = attempts + 1, lastAttemptAt = :now WHERE rowId = :rowId")
    suspend fun incAttempt(rowId: Long, now: Long)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE active = 1 ORDER BY name")
    suspend fun getActive(): List<CategoryEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: CategoryEntity)
    @Query("SELECT * FROM categories ORDER BY name")
    suspend fun getAll(): List<CategoryEntity>
}

@Dao
interface ProductLocalDao {
    @Query("SELECT * FROM products_local WHERE active = 1 ORDER BY name")
    suspend fun getActive(): List<ProductLocalEntity>
    @Query("SELECT * FROM products_local ORDER BY name")
    suspend fun getAll(): List<ProductLocalEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: ProductLocalEntity)
}

@Dao
interface TableDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTable(table: TableEntity)
    @Query("SELECT * FROM tables ORDER BY number")
    suspend fun getAll(): List<TableEntity>
    @Query("UPDATE tables SET status = :status, openedAt = :openedAt WHERE tableId = :tableId")
    suspend fun updateStatus(tableId: String, status: String, openedAt: Long?)
}

@Dao
interface TableOrderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(comanda: TableOrderEntity)
    @Query("SELECT * FROM table_orders WHERE tableId = :tableId AND status = 'ABERTA' LIMIT 1")
    suspend fun getOpenByTable(tableId: String): TableOrderEntity?
    @Query("SELECT * FROM table_orders WHERE status = 'ABERTA' ORDER BY openedAt DESC")
    suspend fun getOpen(): List<TableOrderEntity>
    @Query("UPDATE table_orders SET totalCents = :totalCents WHERE comandaId = :comandaId")
    suspend fun updateTotal(comandaId: String, totalCents: Int)
}

@Dao
interface TableOrderItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: TableOrderItemEntity)
    @Query("SELECT * FROM table_order_items WHERE comandaId = :comandaId ORDER BY addedAt")
    suspend fun getItems(comandaId: String): List<TableOrderItemEntity>
}

