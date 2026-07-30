package com.anilibrix.plus.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.anilibrix.plus.core.database.entity.SyncOperationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncOperationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(operation: SyncOperationEntity): Long

    /**
     * Убирает более ранние операции того же вида по тому же тайтлу.
     *
     * Если статус переключили три раза подряд, до сервера должно доехать
     * последнее значение, а не все три по очереди — иначе в чужом трекере
     * промелькнут состояния, которых пользователь не выбирал.
     */
    @Query("DELETE FROM sync_operations WHERE kind = :kind AND titleId = :titleId")
    suspend fun deleteSupersededBy(kind: String, titleId: Long)

    @Query("SELECT * FROM sync_operations ORDER BY createdAt ASC LIMIT :limit")
    suspend fun peek(limit: Int): List<SyncOperationEntity>

    @Query("SELECT COUNT(*) FROM sync_operations")
    fun countFlow(): Flow<Int>

    @Query("DELETE FROM sync_operations WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE sync_operations SET attempts = attempts + 1 WHERE id = :id")
    suspend fun incrementAttempts(id: Long)

    @Query("DELETE FROM sync_operations WHERE attempts >= :maxAttempts")
    suspend fun dropExhausted(maxAttempts: Int)

    @Query("DELETE FROM sync_operations")
    suspend fun deleteAll()
}
