package br.com.ruy.pos.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String,
    val sku: String?,
    val name: String,
    val category: String?,
    val priceCents: Int,
    val active: Boolean,
    val imageUrl: String?,
    val version: Long,
    val updatedAt: String?,
    val unit: String?
)

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey val id: String,
    val totalCents: Int,
    val paymentStatus: String,
    val syncStatus: String,
    val itemsJson: String,
    val createdAt: Long,
    val operatorId: String?,
    val deviceId: String?,
    val stoneTransactionId: String?
)

@Entity(tableName = "payments")
data class PaymentEntity(
    @PrimaryKey val paymentId: String,
    val orderId: String,
    val amountCents: Int,
    val method: String,
    val status: String,
    val receivedCents: Int? = null,
    val changeCents: Int? = null,
    val installments: Int = 1,
    val transactionRef: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    val orderId: String,
    val payload: String,
    val attempts: Int = 0,
    val lastAttemptAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "tables")
data class TableEntity(
    @PrimaryKey val tableId: String,
    val number: Int,
    val status: String,
    val openedAt: Long? = null,
    val closedAt: Long? = null
)

@Entity(tableName = "table_orders")
data class TableOrderEntity(
    @PrimaryKey val comandaId: String,
    val tableId: String,
    val status: String,
    val totalCents: Int = 0,
    val openedAt: Long = System.currentTimeMillis(),
    val closedAt: Long? = null
)

@Entity(tableName = "table_order_items")
data class TableOrderItemEntity(
    @PrimaryKey val itemId: String,
    val comandaId: String,
    val productId: String?,
    val productName: String,
    val unitCents: Int,
    val qty: Int,
    val addedAt: Long = System.currentTimeMillis()
)

