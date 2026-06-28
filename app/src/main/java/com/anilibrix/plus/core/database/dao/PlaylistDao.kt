package com.anilibrix.plus.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.anilibrix.plus.core.database.entity.PlaylistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(playlist: PlaylistEntity): Long

    @Update
    suspend fun update(playlist: PlaylistEntity)

    @Delete
    suspend fun delete(playlist: PlaylistEntity)

    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAll(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getById(id: Long): PlaylistEntity?
}
