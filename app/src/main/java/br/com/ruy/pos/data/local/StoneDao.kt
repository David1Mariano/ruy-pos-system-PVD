package br.com.ruy.pos.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StoneDao {
    @Insert suspend fun insert(attempt: StoneAttemptEntity): Long
    @Update suspend fun update(attempt: StoneAttemptEntity)
    @Query("SELECT * FROM stone_attempts WHERE stoneOrderId = :reference")
    suspend fun byReference(reference: Long): StoneAttemptEntity?
    @Query("SELECT * FROM stone_attempts WHERE paymentId = :paymentId LIMIT 1")
    suspend fun byPayment(paymentId: String): StoneAttemptEntity?
    @Query("SELECT * FROM stone_attempts WHERE attemptId = :attemptId LIMIT 1")
    suspend fun byAttemptId(attemptId: String): StoneAttemptEntity?
    @Query("SELECT * FROM stone_attempts WHERE activeSlot = 1 LIMIT 1")
    suspend fun active(): StoneAttemptEntity?
    @Query("SELECT * FROM stone_attempts WHERE activeSlot = 1 LIMIT 1")
    fun observeActive(): Flow<StoneAttemptEntity?>
    @Query("SELECT * FROM stone_attempts WHERE orderId = :orderId ORDER BY stoneOrderId DESC")
    fun observeByOrder(orderId: String): Flow<List<StoneAttemptEntity>>
    @Query("SELECT * FROM stone_attempts WHERE atk = :atk AND stoneOrderId != :reference LIMIT 1")
    suspend fun otherWithAtk(atk: String, reference: Long): StoneAttemptEntity?
    @Query("UPDATE stone_attempts SET state = 'AGUARDANDO', updatedAt = :now WHERE stoneOrderId = :reference AND state = 'CRIADA' AND activeSlot = 1")
    suspend fun claimLaunch(reference: Long, now: Long): Int
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun event(event: StoneCallbackEventEntity): Long
    @Query("SELECT * FROM stone_callback_events")
    suspend fun events(): List<StoneCallbackEventEntity>
}
