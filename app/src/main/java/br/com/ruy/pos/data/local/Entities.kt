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

@Entity(tableName = "orders", indices = [androidx.room.Index(value = ["comandaId"], unique = true)])
data class OrderEntity(
    @PrimaryKey val id: String,
    val totalCents: Int,
    val paymentStatus: String,
    val syncStatus: String,
    val itemsJson: String,
    val createdAt: Long,
    val operatorId: String?,
    val deviceId: String?,
    val stoneTransactionId: String?,
    val comandaId: String? = null
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
    val createdAt: Long = System.currentTimeMillis(),
    val cashSessionId: String? = null
)

@Entity(tableName = "cash_sessions")
data class CashSessionEntity(
    @PrimaryKey val sessionId: String,
    val operator: String,
    val openedAt: Long,
    val closedAt: Long? = null,
    val openingBalanceCents: Int,
    val status: String
)

@Entity(tableName = "cash_movements")
data class CashMovementEntity(
    @PrimaryKey val movementId: String,
    val sessionId: String,
    val type: String,
    val amountCents: Int,
    val reason: String?,
    val orderId: String?,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "cash_closings")
data class CashClosingEntity(
    @PrimaryKey val closingId: String,
    val sessionId: String,
    val closedAt: Long,
    val expectedCashCents: Int,
    val countedCashCents: Int,
    val differenceCents: Int,
    val totalSalesCents: Int,
    val totalCashCents: Int,
    val totalPixCents: Int,
    val totalDebitCents: Int,
    val totalCreditCents: Int,
    val sangriasCents: Int,
    val suppliesCents: Int,
    val operator: String,
    val note: String?
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

