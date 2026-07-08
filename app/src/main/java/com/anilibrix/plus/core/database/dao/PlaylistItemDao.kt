package com.anilibrix.plus.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.anilibrix.plus.core.database.entity.PlaylistItemEntity

@Dao
interface PlaylistItemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: PlaylistItemEntity)

    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId AND titleId = :titleId")
    suspend fun delete(playlistId: Long, titleId: Long)

    @Query("SELECT * FROM playlist_items WHERE playlistId = :playlistId")
    suspend fun getByPlaylistId(playlistId: Long): List<PlaylistItemEntity>

    @Query("SELECT COUNT(*) FROM playlist_items WHERE playlistId = :playlistId")
    suspend fun getCount(playlistId: Long): Int

    @Query("DELETE FROM playlist_items")
    suspend fun deleteAll()
}
