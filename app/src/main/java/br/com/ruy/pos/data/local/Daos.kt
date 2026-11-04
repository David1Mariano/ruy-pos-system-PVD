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
    @Query("SELECT * FROM orders WHERE id = :id")
    suspend fun byId(id: String): OrderEntity?
    @Query("SELECT * FROM orders WHERE comandaId = :comandaId LIMIT 1")
    suspend fun byComanda(comandaId: String): OrderEntity?
    @Query("SELECT * FROM orders WHERE id = :id")
    fun observe(id: String): kotlinx.coroutines.flow.Flow<OrderEntity?>
    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    fun observeAll(): kotlinx.coroutines.flow.Flow<List<OrderEntity>>
    @Update suspend fun update(order: OrderEntity)
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
interface CashSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: CashSessionEntity)
    @Query("SELECT * FROM cash_sessions WHERE status = 'ABERTO' LIMIT 1")
    suspend fun getOpen(): CashSessionEntity?
    @Query("UPDATE cash_sessions SET status = 'FECHADO', closedAt = :closedAt WHERE sessionId = :sessionId")
    suspend fun close(sessionId: String, closedAt: Long)
}

@Dao
interface CashMovementDao {
    @Insert
    suspend fun insert(movement: CashMovementEntity)
    @Query("SELECT * FROM cash_movements WHERE sessionId = :sessionId ORDER BY createdAt")
    suspend fun getBySession(sessionId: String): List<CashMovementEntity>
    @Query("SELECT * FROM cash_movements WHERE sessionId = :sessionId ORDER BY createdAt")
    fun observe(sessionId: String): kotlinx.coroutines.flow.Flow<List<CashMovementEntity>>
}

@Dao
interface CashClosingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(closing: CashClosingEntity)
    @Query("SELECT * FROM cash_closings ORDER BY closedAt DESC")
    suspend fun getAll(): List<CashClosingEntity>
}

@Dao
interface PaymentDao {
    @Insert
    suspend fun insert(payment: PaymentEntity)
    @Query("SELECT * FROM payments WHERE orderId = :orderId ORDER BY createdAt DESC")
    suspend fun getByOrder(orderId: String): List<PaymentEntity>
    @Query("SELECT * FROM payments ORDER BY createdAt DESC")
    suspend fun getAll(): List<PaymentEntity>
    @Query("SELECT * FROM payments WHERE paymentId = :id")
    suspend fun byId(id: String): PaymentEntity?
    @Update suspend fun update(payment: PaymentEntity)
    @Query("SELECT * FROM payments WHERE orderId = :orderId ORDER BY createdAt DESC")
    fun observe(orderId: String): kotlinx.coroutines.flow.Flow<List<PaymentEntity>>
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
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun upsertTable(table: TableEntity)
    @Query("SELECT * FROM tables ORDER BY number")
    suspend fun getAll(): List<TableEntity>
    @Query("SELECT * FROM tables WHERE tableId = :id")
    suspend fun byId(id: String): TableEntity?
    @Query("SELECT * FROM tables ORDER BY number")
    fun observeAll(): kotlinx.coroutines.flow.Flow<List<TableEntity>>
    @Query("UPDATE tables SET status = :status, openedAt = :openedAt WHERE tableId = :tableId")
    suspend fun updateStatus(tableId: String, status: String, openedAt: Long?)
}

@Dao
interface TableOrderDao {
    @Query("SELECT * FROM table_orders WHERE comandaId = :id")
    suspend fun byId(id: String): TableOrderEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(comanda: TableOrderEntity)
    @Query("SELECT * FROM table_orders WHERE tableId = :tableId AND status = 'ABERTA' LIMIT 1")
    suspend fun getOpenByTable(tableId: String): TableOrderEntity?
    @Query("SELECT * FROM table_orders WHERE status = 'ABERTA' ORDER BY openedAt DESC")
    suspend fun getOpen(): List<TableOrderEntity>
    @Query("UPDATE table_orders SET totalCents = :totalCents WHERE comandaId = :comandaId")
    suspend fun updateTotal(comandaId: String, totalCents: Int)
    @Query("UPDATE table_orders SET status = :status, closedAt = :closedAt WHERE comandaId = :comandaId")
    suspend fun updateStatus(comandaId: String, status: String, closedAt: Long?)
}

@Dao
interface TableOrderItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: TableOrderItemEntity)
    @Query("SELECT * FROM table_order_items WHERE comandaId = :comandaId ORDER BY addedAt")
    suspend fun getItems(comandaId: String): List<TableOrderItemEntity>
}

