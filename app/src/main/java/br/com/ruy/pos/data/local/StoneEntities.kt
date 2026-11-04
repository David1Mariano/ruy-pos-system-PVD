package br.com.ruy.pos.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** AUTOINCREMENT is a positive Long, never a truncated/hashed order UUID. */
@Entity(tableName = "stone_attempts", indices = [
    Index(value = ["attemptId"], unique = true),
    Index(value = ["paymentId"], unique = true),
    Index(value = ["activeSlot"], unique = true),
    Index(value = ["atk"], unique = true)
])
data class StoneAttemptEntity(
    @PrimaryKey(autoGenerate = true) val stoneOrderId: Long = 0,
    val attemptId: String,
    val paymentId: String,
    val orderId: String,
    val installmentType: String,
    val origin: String = "DEEPLINK",
    val state: String = "CRIADA",
    // A terminal has one flight at a time. NULL releases the unique lock.
    val activeSlot: Int? = 1,
    val atk: String? = null,
    val itk: String? = null,
    val authorizationCode: String? = null,
    val authorizationDateTime: String? = null,
    val returnCode: String? = null,
    val detail: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val reconciledBy: String? = null,
    val reconciliationEvidence: String? = null
)

/** Allowlisted callback audit; never persist PAN, cardholder name or raw URIs. */
@Entity(tableName = "stone_callback_events")
data class StoneCallbackEventEntity(
    @PrimaryKey val fingerprint: String,
    val attemptId: String?,
    val result: String,
    val safeFields: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
