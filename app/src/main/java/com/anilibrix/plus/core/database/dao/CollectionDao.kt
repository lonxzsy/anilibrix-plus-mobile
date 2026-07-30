package com.anilibrix.plus.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.anilibrix.plus.core.database.entity.CollectionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(collection: CollectionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(collections: List<CollectionEntity>)

    @Query("DELETE FROM collections WHERE titleId = :titleId AND collectionType = :collectionType")
    suspend fun delete(titleId: Long, collectionType: String)

    @Query("DELETE FROM collections WHERE titleId = :titleId")
    suspend fun deleteAllForTitle(titleId: Long)

    /**
     * Ставит тайтлу ровно один статус.
     *
     * Статусы взаимоисключающие по смыслу: нельзя одновременно «смотрю» и
     * «брошено». Раньше ограничения не было вовсе — наружу был выведен только
     * WATCH_LATER, поэтому конфликтовать было нечему. С пятью статусами
     * атомарная замена обязательна, иначе тайтл окажется сразу в двух вкладках
     * библиотеки.
     */
    @Transaction
    suspend fun setExclusiveStatus(collection: CollectionEntity) {
        deleteAllForTitle(collection.titleId)
        insert(collection)
    }

    @Query("SELECT * FROM collections WHERE collectionType = :collectionType ORDER BY updatedAt DESC, addedAt DESC")
    fun getByType(collectionType: String): Flow<List<CollectionEntity>>

    @Query("SELECT * FROM collections ORDER BY addedAt DESC")
    fun getAll(): Flow<List<CollectionEntity>>

    @Query("SELECT * FROM collections")
    suspend fun getAllOnce(): List<CollectionEntity>

    @Query("SELECT * FROM collections WHERE titleId = :titleId LIMIT 1")
    suspend fun getForTitle(titleId: Long): CollectionEntity?

    @Query("SELECT * FROM collections WHERE titleId = :titleId LIMIT 1")
    fun getForTitleFlow(titleId: Long): Flow<CollectionEntity?>

    @Query("SELECT EXISTS(SELECT 1 FROM collections WHERE titleId = :titleId AND collectionType = :collectionType)")
    suspend fun exists(titleId: Long, collectionType: String): Boolean

    @Query("SELECT collectionType FROM collections WHERE titleId = :titleId")
    suspend fun getTypesForTitle(titleId: Long): List<String>

    @Query("SELECT collectionType, COUNT(*) AS count FROM collections GROUP BY collectionType")
    fun getCounts(): Flow<List<CollectionTypeCount>>

    @Query("UPDATE collections SET progressEpisode = :episode, updatedAt = :updatedAt WHERE titleId = :titleId")
    suspend fun updateProgress(titleId: Long, episode: Int, updatedAt: Long)

    @Query("UPDATE collections SET shikimoriId = :shikimoriId WHERE titleId = :titleId")
    suspend fun updateShikimoriId(titleId: Long, shikimoriId: Int?)

    @Query("DELETE FROM collections WHERE collectionType = :collectionType")
    suspend fun deleteAllOfType(collectionType: String)

    @Query("DELETE FROM collections")
    suspend fun deleteAll()
}

data class CollectionTypeCount(
    val collectionType: String,
    val count: Int,
)
