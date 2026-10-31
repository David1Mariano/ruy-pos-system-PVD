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

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    val orderId: String,
    val payload: String,
    val attempts: Int = 0,
    val lastAttemptAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
