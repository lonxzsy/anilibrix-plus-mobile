package com.anilibrix.plus.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.anilibrix.plus.core.database.entity.RatingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RatingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rating: RatingEntity)

    @Query("DELETE FROM ratings WHERE titleId = :titleId")
    suspend fun delete(titleId: Long)

    @Query("SELECT * FROM ratings WHERE titleId = :titleId")
    suspend fun getByTitleId(titleId: Long): RatingEntity?

    @Query("SELECT * FROM ratings")
    fun getAll(): Flow<List<RatingEntity>>

    @Query("DELETE FROM ratings")
    suspend fun deleteAll()
}
