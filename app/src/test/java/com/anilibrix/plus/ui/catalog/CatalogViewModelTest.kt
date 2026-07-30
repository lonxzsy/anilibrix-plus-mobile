package com.anilibrix.plus.ui.catalog

import com.anilibrix.plus.domain.model.Genre
import com.anilibrix.plus.domain.model.NetworkResult
import com.anilibrix.plus.domain.model.Poster
import com.anilibrix.plus.domain.model.Title
import com.anilibrix.plus.domain.model.TitleName
import com.anilibrix.plus.domain.repository.AnilibriaRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CatalogViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: CatalogViewModel
    private val repository = mockk<AnilibriaRepository>()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() {
        every { repository.getCatalog(any(), any(), any()) } returns flowOf(NetworkResult.Success(emptyList()))
        viewModel = CatalogViewModel(repository)
    }

    @Test
    fun initialState_isLoading() {
        createViewModel()

        assertTrue(viewModel.state.value.loading)
    }

    @Test
    fun onIntent_ClearSearch_resetsSearchAndReloads() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.onIntent(CatalogIntent.SubmitSearch("test"))
        advanceUntilIdle()

        viewModel.onIntent(CatalogIntent.ClearSearch)
        advanceUntilIdle()

        assertEquals("", viewModel.state.value.filter.search)
        assertTrue(viewModel.state.value.suggestions.isEmpty())
    }

    @Test
    fun onIntent_ToggleViewMode_updatesMode() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.onIntent(CatalogIntent.ToggleViewMode(ViewMode.LIST))

        assertEquals(ViewMode.LIST, viewModel.state.value.filter.viewMode)
    }

    @Test
    fun onIntent_UpdateFilter_updatesFilter() = runTest {
        createViewModel()
        advanceUntilIdle()

        val newFilter = CatalogFilter(
            search = "naruto",
            genres = setOf("Action"),
            year = 2023
        )
        viewModel.onIntent(CatalogIntent.UpdateFilter(newFilter))

        assertEquals("naruto", viewModel.state.value.filter.search)
        assertEquals(setOf("Action"), viewModel.state.value.filter.genres)
        assertEquals(2023, viewModel.state.value.filter.year)
    }

    @Test
    fun loadCatalog_onSuccess_updatesTitles() = runTest {
        val titles = listOf(
            Title(
                id = 1L,
                alias = "test-1",
                name = TitleName(main = "Test 1"),
                poster = Poster(small = "http://poster1.jpg")
            ),
            Title(
                id = 2L,
                alias = "test-2",
                name = TitleName(main = "Test 2"),
                poster = Poster(small = "http://poster2.jpg")
            )
        )
        every { repository.getCatalog(1, 20, null) } returns flowOf(NetworkResult.Success(titles))

        viewModel = CatalogViewModel(repository)
        advanceUntilIdle()

        assertEquals(2, viewModel.state.value.titles.size)
        assertFalse(viewModel.state.value.loading)
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun loadCatalog_onError_setsError() = runTest {
        every { repository.getCatalog(any(), any(), any()) } returns flowOf(
            NetworkResult.Error("Network error")
        )

        viewModel = CatalogViewModel(repository)
        advanceUntilIdle()

        assertEquals("Network error", viewModel.state.value.error)
        assertFalse(viewModel.state.value.loading)
    }

    @Test
    fun loadCatalog_withSearch_passesSearchToRepository() = runTest {
        every { repository.getCatalog(1, 20, "naruto") } returns flowOf(NetworkResult.Success(emptyList()))

        viewModel = CatalogViewModel(repository)
        advanceUntilIdle()

        viewModel.onIntent(CatalogIntent.SubmitSearch("naruto"))
        advanceUntilIdle()

        io.mockk.verify { repository.getCatalog(1, 20, "naruto") }
    }

    @Test
    fun loadMore_incrementsPage() = runTest {
        val titles = listOf(
            Title(id = 1L, alias = "test", name = TitleName(main = "Test"))
        )
        every { repository.getCatalog(1, 20, null) } returns flowOf(NetworkResult.Success(titles))
        every { repository.getCatalog(2, 20, null) } returns flowOf(NetworkResult.Success(emptyList()))

        viewModel = CatalogViewModel(repository)
        advanceUntilIdle()

        viewModel.onIntent(CatalogIntent.LoadMore)
        advanceUntilIdle()

        assertEquals(2, viewModel.state.value.currentPage)
    }

    @Test
    fun loadMore_whenNoMoreData_doesNotLoad() = runTest {
        every { repository.getCatalog(any(), any(), any()) } returns flowOf(NetworkResult.Success(emptyList()))

        viewModel = CatalogViewModel(repository)
        advanceUntilIdle()

        viewModel.onIntent(CatalogIntent.LoadMore)
        advanceUntilIdle()

        assertEquals(1, viewModel.state.value.currentPage)
    }
}
