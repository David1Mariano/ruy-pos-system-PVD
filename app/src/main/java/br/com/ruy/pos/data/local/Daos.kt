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
