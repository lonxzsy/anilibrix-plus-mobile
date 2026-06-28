package com.anilibrix.plus.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.anilibrix.plus.core.database.entity.FavoriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE titleId = :titleId")
    suspend fun deleteByTitleId(titleId: Long)

    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun getAll(): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites WHERE titleId = :titleId")
    suspend fun getById(titleId: Long): FavoriteEntity?

    @Query("SELECT COUNT(*) FROM favorites")
    fun getCount(): Flow<Int>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE titleId = :titleId)")
    suspend fun isFavorite(titleId: Long): Boolean
}
