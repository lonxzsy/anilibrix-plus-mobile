package com.anilibrix.plus.data.repository

import com.anilibrix.plus.core.database.dao.CollectionDao
import com.anilibrix.plus.core.database.dao.FavoriteDao
import com.anilibrix.plus.core.database.dao.HistoryDao
import com.anilibrix.plus.core.database.dao.PlaylistDao
import com.anilibrix.plus.core.database.dao.PlaylistItemDao
import com.anilibrix.plus.core.database.dao.RatingDao
import com.anilibrix.plus.core.database.dao.WatchLaterDao
import com.anilibrix.plus.core.database.entity.FavoriteEntity
import com.anilibrix.plus.core.database.entity.HistoryEntity
import com.anilibrix.plus.domain.model.CollectionType
import com.anilibrix.plus.domain.model.HistoryEntry
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LocalRepositoryImplTest {

    private lateinit var repository: LocalRepositoryImpl
    private val favoriteDao = mockk<FavoriteDao>()
    private val historyDao = mockk<HistoryDao>()
    private val playlistDao = mockk<PlaylistDao>()
    private val playlistItemDao = mockk<PlaylistItemDao>()
    private val watchLaterDao = mockk<WatchLaterDao>()
    private val ratingDao = mockk<RatingDao>()
    private val collectionDao = mockk<CollectionDao>()

    @Before
    fun setup() {
        repository = LocalRepositoryImpl(
            favoriteDao = favoriteDao,
            historyDao = historyDao,
            playlistDao = playlistDao,
            playlistItemDao = playlistItemDao,
            watchLaterDao = watchLaterDao,
            ratingDao = ratingDao,
            collectionDao = collectionDao
        )
    }

    @Test
    fun getFavorites_returnsMappedTitles() = runTest {
        val entities = listOf(
            FavoriteEntity(titleId = 1L, titleName = "Title 1", posterUrl = null),
            FavoriteEntity(titleId = 2L, titleName = "Title 2", posterUrl = "http://poster.jpg")
        )
        every { favoriteDao.getAll() } returns flowOf(entities)

        val favorites = repository.getFavorites().first()

        assertEquals(2, favorites.size)
        assertEquals(1L, favorites[0].titleId)
        assertEquals("Title 1", favorites[0].titleName)
        assertEquals(2L, favorites[1].titleId)
    }

    @Test
    fun isFavorite_returnsTrueWhenExists() = runTest {
        every { favoriteDao.isFavorite(1L) } returns true

        assertTrue(repository.isFavorite(1L))
    }

    @Test
    fun isFavorite_returnsFalseWhenNotExists() = runTest {
        every { favoriteDao.isFavorite(1L) } returns false

        assertFalse(repository.isFavorite(1L))
    }

    @Test
    fun addFavorite_insertsEntity() = runTest {
        val entitySlot = slot<FavoriteEntity>()
        every { favoriteDao.insert(capture(entitySlot)) } returns Unit

        repository.addFavorite(1L, "Test Title", "http://poster.jpg")

        verify { favoriteDao.insert(any()) }
        assertEquals(1L, entitySlot.captured.titleId)
        assertEquals("Test Title", entitySlot.captured.titleName)
    }

    @Test
    fun removeFavorite_deletesByTitleId() = runTest {
        every { favoriteDao.deleteByTitleId(1L) } returns Unit

        repository.removeFavorite(1L)

        verify { favoriteDao.deleteByTitleId(1L) }
    }

    @Test
    fun getHistory_returnsMappedEntries() = runTest {
        val entities = listOf(
            HistoryEntity(
                titleId = 1L,
                episodeId = 10L,
                episodeNumber = 1,
                timestamp = 120000L,
                duration = 240000L,
                watchedAt = System.currentTimeMillis(),
                titleName = "Title",
                posterUrl = null
            )
        )
        every { historyDao.getAll() } returns flowOf(entities)

        val history = repository.getHistory().first()

        assertEquals(1, history.size)
        assertEquals(1L, history[0].titleId)
        assertEquals(10L, history[0].episodeId)
        assertEquals(120000L, history[0].timestamp)
    }

    @Test
    fun addHistory_insertsEntity() = runTest {
        val entry = HistoryEntry(
            titleId = 1L,
            episodeId = 10L,
            episodeNumber = 1,
            timestamp = 120000L,
            duration = 240000L,
            watchedAt = System.currentTimeMillis(),
            titleName = "Title",
            posterUrl = null
        )
        every { historyDao.insert(any()) } returns Unit

        repository.addHistory(entry)

        verify { historyDao.insert(any()) }
    }

    @Test
    fun clearHistory_deletesAll() = runTest {
        every { historyDao.deleteAll() } returns Unit

        repository.clearHistory()

        verify { historyDao.deleteAll() }
    }

    @Test
    fun clearAccountData_clearsAllData() = runTest {
        every { favoriteDao.deleteAll() } returns Unit
        every { watchLaterDao.deleteAll() } returns Unit
        every { collectionDao.deleteAll() } returns Unit
        every { ratingDao.deleteAll() } returns Unit
        every { historyDao.deleteAll() } returns Unit
        every { playlistItemDao.deleteAll() } returns Unit
        every { playlistDao.deleteAll() } returns Unit

        repository.clearAccountData()

        verify { favoriteDao.deleteAll() }
        verify { watchLaterDao.deleteAll() }
        verify { collectionDao.deleteAll() }
        verify { ratingDao.deleteAll() }
        verify { historyDao.deleteAll() }
        verify { playlistItemDao.deleteAll() }
        verify { playlistDao.deleteAll() }
    }

    @Test
    fun addWatchLater_insertsEntity() = runTest {
        every { watchLaterDao.insert(any()) } returns Unit

        repository.addWatchLater(1L, "Title", "http://poster.jpg")

        verify { watchLaterDao.insert(any()) }
    }

    @Test
    fun removeWatchLater_deletesEntity() = runTest {
        every { watchLaterDao.delete(1L) } returns Unit

        repository.removeWatchLater(1L)

        verify { watchLaterDao.delete(1L) }
    }
}
