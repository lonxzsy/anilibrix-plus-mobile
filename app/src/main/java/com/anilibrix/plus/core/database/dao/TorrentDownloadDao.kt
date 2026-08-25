package com.anilibrix.plus.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.anilibrix.plus.core.database.entity.TorrentDownloadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TorrentDownloadDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(torrent: TorrentDownloadEntity)

    @Update
    suspend fun update(torrent: TorrentDownloadEntity)

    @Query("DELETE FROM torrent_downloads WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM torrent_downloads WHERE titleId = :titleId")
    suspend fun deleteByTitle(titleId: Long)

    @Query("SELECT * FROM torrent_downloads ORDER BY createdAt DESC")
    fun getAll(): Flow<List<TorrentDownloadEntity>>

    @Query("SELECT * FROM torrent_downloads WHERE id = :id")
    suspend fun getById(id: String): TorrentDownloadEntity?

    @Query("SELECT * FROM torrent_downloads WHERE titleId = :titleId ORDER BY createdAt DESC")
    fun getByTitle(titleId: Long): Flow<List<TorrentDownloadEntity>>

    @Query("DELETE FROM torrent_downloads")
    suspend fun deleteAll()
}
