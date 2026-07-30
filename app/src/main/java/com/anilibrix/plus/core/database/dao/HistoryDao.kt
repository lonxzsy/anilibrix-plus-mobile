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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(history: List<HistoryEntity>)

    @Query("DELETE FROM history WHERE titleId = :titleId AND episodeId = :episodeId")
    suspend fun delete(titleId: Long, episodeId: Long)

    @Query("SELECT * FROM history WHERE titleId = :titleId AND episodeId = :episodeId")
    suspend fun getEntry(titleId: Long, episodeId: Long): HistoryEntity?

    @Query("SELECT * FROM history ORDER BY watchedAt DESC LIMIT 500")
    fun getAll(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history WHERE titleId = :titleId ORDER BY episodeNumber")
    fun getByTitleId(titleId: Long): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history WHERE titleId = :titleId ORDER BY episodeNumber")
    suspend fun getByTitleIdOnce(titleId: Long): List<HistoryEntity>

    @Query("SELECT * FROM history ORDER BY watchedAt DESC LIMIT 1")
    suspend fun getLatest(): HistoryEntity?

    /**
     * Последняя запись по каждому тайтлу — ровно то, что нужно секции
     * «Продолжить просмотр» и напоминаниям о недосмотренном.
     *
     * Группировка с `MAX(watchedAt)` вместо выборки всего и отбора в Kotlin:
     * история может быть длинной, а нужна из неё одна строка на тайтл.
     */
    @Query(
        """
        SELECT * FROM history
        WHERE watchedAt IN (SELECT MAX(watchedAt) FROM history GROUP BY titleId)
        ORDER BY watchedAt DESC
        """
    )
    fun getLatestPerTitle(): Flow<List<HistoryEntity>>

    /**
     * Суммарное время просмотра в миллисекундах.
     *
     * Досмотренная серия засчитывается целиком, недосмотренная — по позиции
     * остановки. Раньше профиль складывал позиции возобновления и показывал
     * заметно заниженное число: серия, досмотренная до конца, вносила в сумму
     * ноль, потому что позиция при завершении не сохраняется.
     */
    @Query(
        """
        SELECT COALESCE(SUM(
            CASE WHEN duration > 0 AND timestamp * 100 >= duration * 90
                 THEN duration ELSE timestamp END
        ), 0) FROM history
        """
    )
    suspend fun getTotalWatchTimeMs(): Long

    @Query("SELECT COUNT(*) FROM history")
    suspend fun getCount(): Int

    @Query("DELETE FROM history")
    suspend fun deleteAll()
}
