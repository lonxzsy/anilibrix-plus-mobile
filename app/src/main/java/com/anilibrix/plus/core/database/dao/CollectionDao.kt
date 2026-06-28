package com.anilibrix.plus.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.anilibrix.plus.core.database.entity.CollectionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(collection: CollectionEntity)

    @Query("DELETE FROM collections WHERE titleId = :titleId AND collectionType = :collectionType")
    suspend fun delete(titleId: Long, collectionType: String)

    @Query("DELETE FROM collections WHERE titleId = :titleId")
    suspend fun deleteAllForTitle(titleId: Long)

    @Query("SELECT * FROM collections WHERE collectionType = :collectionType ORDER BY addedAt DESC")
    fun getByType(collectionType: String): Flow<List<CollectionEntity>>

    @Query("SELECT * FROM collections ORDER BY addedAt DESC")
    fun getAll(): Flow<List<CollectionEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM collections WHERE titleId = :titleId AND collectionType = :collectionType)")
    suspend fun exists(titleId: Long, collectionType: String): Boolean

    @Query("SELECT collectionType FROM collections WHERE titleId = :titleId")
    suspend fun getTypesForTitle(titleId: Long): List<String>

    @Query("DELETE FROM collections WHERE collectionType = :collectionType")
    suspend fun deleteAllOfType(collectionType: String)
}
