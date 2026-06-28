package com.anilibrix.plus.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.anilibrix.plus.core.database.entity.HistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: HistoryEntity)

    @Query("DELETE FROM history WHERE titleId = :titleId AND episodeId = :episodeId")
    suspend fun delete(titleId: Long, episodeId: Long)

    @Query("SELECT * FROM history ORDER BY watchedAt DESC LIMIT 500")
    fun getAll(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history WHERE titleId = :titleId ORDER BY watchedAt DESC")
    fun getByTitleId(titleId: Long): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history ORDER BY watchedAt DESC LIMIT 1")
    suspend fun getLatest(): HistoryEntity?

    @Query("DELETE FROM history")
    suspend fun deleteAll()
}
