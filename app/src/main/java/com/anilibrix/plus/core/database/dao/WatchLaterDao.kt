package com.anilibrix.plus.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.anilibrix.plus.core.database.entity.WatchLaterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchLaterDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(watchLater: WatchLaterEntity)

    @Query("DELETE FROM watch_later WHERE titleId = :titleId")
    suspend fun delete(titleId: Long)

    @Query("SELECT * FROM watch_later")
    fun getAll(): Flow<List<WatchLaterEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM watch_later WHERE titleId = :titleId)")
    suspend fun isInWatchLater(titleId: Long): Boolean

    @Query("DELETE FROM watch_later")
    suspend fun deleteAll()
}
