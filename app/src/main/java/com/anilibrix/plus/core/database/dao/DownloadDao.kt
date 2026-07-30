package com.anilibrix.plus.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.anilibrix.plus.core.database.entity.DownloadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(download: DownloadEntity)

    @Query("DELETE FROM downloads WHERE requestId = :requestId")
    suspend fun delete(requestId: String)

    @Query("DELETE FROM downloads WHERE titleId = :titleId")
    suspend fun deleteByTitle(titleId: Long)

    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun getAll(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE requestId = :requestId")
    suspend fun getByRequestId(requestId: String): DownloadEntity?

    @Query("SELECT * FROM downloads WHERE titleId = :titleId ORDER BY episodeNumber")
    fun getByTitle(titleId: Long): Flow<List<DownloadEntity>>

    @Query("DELETE FROM downloads")
    suspend fun deleteAll()
}
