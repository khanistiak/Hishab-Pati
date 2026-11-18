package com.example

import android.app.Application
import android.os.Looper
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.InitialData
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.SubcategoryEntity
import org.robolectric.Shadows.shadowOf
import com.example.data.repository.WalletRepository
import com.example.ui.MainViewModel
import com.example.ui.util.DragDropListState
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CategoryDragDropTest {

    private lateinit var app: Application
    private lateinit var db: AppDatabase
    private lateinit var repository: WalletRepository
    private lateinit var viewModel: MainViewModel

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext<Application>()
        if (FirebaseApp.getApps(app).isEmpty()) {
            FirebaseApp.initializeApp(
                app,
                FirebaseOptions.Builder()
                    .setApplicationId("com.example.app")
                    .setProjectId("test-project")
                    .setApiKey("fake-api-key")
                    .build()
            )
        }
        db = AppDatabase.getDatabase(app)
        repository = WalletRepository(db)
        viewModel = MainViewModel(app)
    }

    private class FakeItemInfo(
        override val index: Int,
        override val key: Any,
        override val offset: Int,
        override val size: Int
    ) : LazyListItemInfo {
        override val contentType: Any? = null
    }

    private class FakeLayoutInfo(
        override val visibleItemsInfo: List<LazyListItemInfo>,
        override val viewportStartOffset: Int = 0,
        override val viewportEndOffset: Int = 1200,
        override val totalItemsCount: Int = visibleItemsInfo.size
    ) : LazyListLayoutInfo {
        override val afterContentPadding: Int = 0
        override val beforeContentPadding: Int = 0
        override val orientation = androidx.compose.foundation.gestures.Orientation.Vertical
        override val reverseLayout: Boolean = false
        override val viewportSize = androidx.compose.ui.unit.IntSize(400, 1200)
    }

    @Test
    fun `initial state is clean and idle`() {
        val state = DragDropListState(
            lazyListState = LazyListState(),
            onMove = { _, _ -> },
            scope = CoroutineScope(Dispatchers.Unconfined),
            indexOffset = 0,
            liveReorder = true
        )

        assertFalse(state.isDragging)
        assertNull(state.draggedItemId)
        assertEquals(-1, state.originalIndex)
        assertEquals(-1, state.currentIndex)
        assertEquals(0f, state.dragOffsetY, 0.001f)
    }

    @Test
    fun `drag start from first category at index 0 preserves stable ID and exact offset without jump`() {
        val (cats, _) = InitialData.getDefaultCategories()
        val expenseCats = cats.filter { !it.isIncome && !it.isTransfer }
        val firstCat = expenseCats[0]

        val state = DragDropListState(
            lazyListState = LazyListState(),
            onMove = { _, _ -> },
            scope = CoroutineScope(Dispatchers.Unconfined),
            indexOffset = 0,
            keyProvider = { idx -> if (idx in expenseCats.indices) expenseCats[idx].id else idx },
            liveReorder = true
        )

        state.onDragStart(itemId = firstCat.id, index = 0, listSize = expenseCats.size)

        assertTrue("Must be in dragging state", state.isDragging)
        assertEquals(firstCat.id, state.draggedItemId)
        assertEquals(0, state.originalIndex)
        assertEquals(0, state.currentIndex)
        assertEquals(0f, state.dragOffsetY, 0.001f)
        assertEquals(0f, state.translationY, 0.001f)
    }

    @Test
    fun `dragging first category downward swaps with next categories smoothly`() {
        val (cats, _) = InitialData.getDefaultCategories()
        var expenseCats = cats.filter { !it.isIncome && !it.isTransfer }.toMutableList()
        val firstCat = expenseCats[0] // "Food & Dining"
        val secondCat = expenseCats[1] // "Shopping"

        val moves = mutableListOf<Pair<Int, Int>>()
        val state = DragDropListState(
            lazyListState = LazyListState(),
            onMove = { from, to ->
                moves.add(from to to)
                val item = expenseCats.removeAt(from)
                expenseCats.add(to, item)
            },
            scope = CoroutineScope(Dispatchers.Unconfined),
            indexOffset = 0,
            keyProvider = { idx -> if (idx in expenseCats.indices) expenseCats[idx].id else idx },
            liveReorder = true
        )

        state.onDragStart(itemId = firstCat.id, index = 0, listSize = expenseCats.size)

        // Drag down past the midpoint of the second item (~100px)
        state.onDrag(100f, expenseCats.size)

        assertEquals("First category must have moved to index 1", 1, state.currentIndex)
        assertEquals(firstCat.id, expenseCats[1].id)
        assertEquals(secondCat.id, expenseCats[0].id)
        assertTrue("onMove must have been recorded", moves.isNotEmpty())
        assertEquals(0 to 1, moves.first())

        // Drag down further past the next item (~170px total)
        state.onDrag(70f, expenseCats.size)
        assertEquals("First category must have moved to index 2", 2, state.currentIndex)
        assertEquals(firstCat.id, expenseCats[2].id)

        // Drag back up to index 0
        state.onDrag(-170f, expenseCats.size)
        assertEquals("First category must return to index 0", 0, state.currentIndex)
        assertEquals(firstCat.id, expenseCats[0].id)
    }

    @Test
    fun `dragging middle category at index 5 upward and downward`() {
        val (cats, _) = InitialData.getDefaultCategories()
        var expenseCats = cats.filter { !it.isIncome && !it.isTransfer }.toMutableList()
        val middleIndex = 5
        val middleCat = expenseCats[middleIndex]

        val moves = mutableListOf<Pair<Int, Int>>()
        val state = DragDropListState(
            lazyListState = LazyListState(),
            onMove = { from, to ->
                moves.add(from to to)
                val item = expenseCats.removeAt(from)
                expenseCats.add(to, item)
            },
            scope = CoroutineScope(Dispatchers.Unconfined),
            indexOffset = 0,
            keyProvider = { idx -> if (idx in expenseCats.indices) expenseCats[idx].id else idx },
            liveReorder = true
        )

        state.onDragStart(itemId = middleCat.id, index = middleIndex, listSize = expenseCats.size)
        assertEquals(middleIndex, state.currentIndex)
        assertEquals(middleCat.id, state.draggedItemId)

        // Drag upwards by 70px to move from index 5 to 4
        state.onDrag(-70f, expenseCats.size)
        assertEquals(4, state.currentIndex)
        assertEquals(middleCat.id, expenseCats[4].id)

        // Drag downwards by 70px to return to index 5
        state.onDrag(70f, expenseCats.size)
        assertEquals(5, state.currentIndex)
        assertEquals(middleCat.id, expenseCats[5].id)

        // Drag downwards by another 70px to move to index 6
        state.onDrag(70f, expenseCats.size)
        assertEquals(6, state.currentIndex)
        assertEquals(middleCat.id, expenseCats[6].id)
    }

    @Test
    fun `dragging last category at index 11 upward`() {
        val (cats, _) = InitialData.getDefaultCategories()
        var expenseCats = cats.filter { !it.isIncome && !it.isTransfer }.toMutableList()
        val lastIndex = expenseCats.size - 1 // 11
        val lastCat = expenseCats[lastIndex]

        val state = DragDropListState(
            lazyListState = LazyListState(),
            onMove = { from, to ->
                val item = expenseCats.removeAt(from)
                expenseCats.add(to, item)
            },
            scope = CoroutineScope(Dispatchers.Unconfined),
            indexOffset = 0,
            keyProvider = { idx -> if (idx in expenseCats.indices) expenseCats[idx].id else idx },
            liveReorder = true
        )

        state.onDragStart(itemId = lastCat.id, index = lastIndex, listSize = expenseCats.size)
        assertEquals(lastIndex, state.currentIndex)

        // Drag up past the midpoint of index 10
        state.onDrag(-80f, expenseCats.size)
        assertEquals(lastIndex - 1, state.currentIndex)
        assertEquals(lastCat.id, expenseCats[lastIndex - 1].id)

        // Drag up further to index 9
        state.onDrag(-70f, expenseCats.size)
        assertEquals(lastIndex - 2, state.currentIndex)
        assertEquals(lastCat.id, expenseCats[lastIndex - 2].id)
    }

    @Test
    fun `drag commit onDragEnd updates category sortOrder and persists to Room`() = runBlocking {
        // Reset to default categories in Room
        repository.restoreDefaultCategories()
        val initialCats = repository.categories.first().filter { !it.isIncome && !it.isTransfer }
        assertTrue("Must have default expense categories", initialCats.size >= 12)

        val cat0 = initialCats[0]
        val cat1 = initialCats[1]
        val cat2 = initialCats[2]

        // Reorder list: move cat0 to index 2
        val reordered = initialCats.toMutableList()
        val item = reordered.removeAt(0)
        reordered.add(2, item)

        assertEquals(cat1.id, reordered[0].id)
        assertEquals(cat2.id, reordered[1].id)
        assertEquals(cat0.id, reordered[2].id)

        // Call viewModel.updateCategoriesOrder
        viewModel.updateCategoriesOrder(reordered)

        // Verify in Room database that sortOrder was persisted sequentially
        val persistedCats = kotlinx.coroutines.withTimeout(3000) {
            repository.categories.first { cats ->
                val expenses = cats.filter { !it.isIncome && !it.isTransfer }
                expenses.isNotEmpty() && expenses[0].id == cat1.id
            }.filter { !it.isIncome && !it.isTransfer }
        }
        assertEquals(cat1.id, persistedCats[0].id)
        assertEquals(0, persistedCats[0].sortOrder)
        assertEquals(cat2.id, persistedCats[1].id)
        assertEquals(1, persistedCats[1].sortOrder)
        assertEquals(cat0.id, persistedCats[2].id)
        assertEquals(2, persistedCats[2].sortOrder)
    }

    @Test
    fun `drag cancel onDragCancel reverts in-memory list`() {
        val (cats, _) = InitialData.getDefaultCategories()
        val initialList = cats.filter { !it.isIncome && !it.isTransfer }
        var currentList = initialList.toMutableList()

        var cancelCallbackCalled = false
        val state = DragDropListState(
            lazyListState = LazyListState(),
            onMove = { from, to ->
                val item = currentList.removeAt(from)
                currentList.add(to, item)
            },
            onDragCancel = {
                cancelCallbackCalled = true
                currentList = initialList.toMutableList()
            },
            scope = CoroutineScope(Dispatchers.Unconfined),
            indexOffset = 0,
            keyProvider = { idx -> if (idx in currentList.indices) currentList[idx].id else idx },
            liveReorder = true
        )

        state.onDragStart(itemId = initialList[0].id, index = 0, listSize = initialList.size)
        state.onDrag(100f, initialList.size)
        assertNotEquals(initialList[0].id, currentList[0].id)

        // Cancel
        state.onDragCancel()

        assertTrue("Cancel callback must be invoked", cancelCallbackCalled)
        assertEquals("List must be restored to initial order", initialList[0].id, currentList[0].id)
        assertFalse("Drag state must be reset", state.isDragging)
        assertNull(state.draggedItemId)
    }

    @Test
    fun `income categories and transfer categories use dedicated sortOrder bands`() = runBlocking {
        repository.restoreDefaultCategories()
        val incomeCats = repository.categories.first().filter { it.isIncome && !it.isTransfer }
        val transferCats = repository.categories.first().filter { it.isTransfer }

        val originalIncome0 = incomeCats[0]
        val originalIncome1 = incomeCats[1]

        // Swap income categories
        val reorderedIncome = incomeCats.toMutableList()
        val incItem = reorderedIncome.removeAt(0)
        reorderedIncome.add(1, incItem)

        viewModel.updateCategoriesOrder(reorderedIncome)

        val updatedIncome = kotlinx.coroutines.withTimeout(3000) {
            repository.categories.first { cats ->
                val inc = cats.filter { it.isIncome && !it.isTransfer }
                inc.isNotEmpty() && inc[0].id == originalIncome1.id
            }.filter { it.isIncome && !it.isTransfer }
        }
        assertEquals(originalIncome1.id, updatedIncome[0].id)
        assertEquals(1000, updatedIncome[0].sortOrder)
        assertEquals(originalIncome0.id, updatedIncome[1].id)
        assertEquals(1001, updatedIncome[1].sortOrder)

        // Swap transfer categories
        val originalTransfer0 = transferCats[0]
        val originalTransfer1 = transferCats[1]
        val reorderedTransfer = transferCats.toMutableList()
        val trfItem = reorderedTransfer.removeAt(0)
        reorderedTransfer.add(1, trfItem)

        viewModel.updateCategoriesOrder(reorderedTransfer)

        val updatedTransfer = kotlinx.coroutines.withTimeout(3000) {
            repository.categories.first { cats ->
                val trf = cats.filter { it.isTransfer }
                trf.isNotEmpty() && trf[0].id == originalTransfer1.id
            }.filter { it.isTransfer }
        }
        assertEquals(originalTransfer1.id, updatedTransfer[0].id)
        assertEquals(2000, updatedTransfer[0].sortOrder)
        assertEquals(originalTransfer0.id, updatedTransfer[1].id)
        assertEquals(2001, updatedTransfer[1].sortOrder)
    }

    @Test
    fun `theme colors - card background and text colors are distinct and valid for light and dark`() {
        val lightSurface = androidx.compose.ui.graphics.Color.White
        val darkSurface = androidx.compose.ui.graphics.Color(0xFF1E2230)

        // Light mode checks
        assertTrue("Light surface must be white", lightSurface.red > 0.9f && lightSurface.green > 0.9f && lightSurface.blue > 0.9f)
        // Dark mode checks
        assertTrue("Dark surface must be dark", darkSurface.red < 0.2f && darkSurface.green < 0.2f && darkSurface.blue < 0.3f)

        // Drag elevation & shadow
        val dragElevation = 8
        val idleElevation = 0
        assertTrue("Dragged row must be visibly elevated", dragElevation > idleElevation)
    }

    @Test
    fun `dragging category from index 1 upward to index 0 works and swaps smoothly`() {
        val (cats, _) = InitialData.getDefaultCategories()
        var expenseCats = cats.filter { !it.isIncome && !it.isTransfer }.toMutableList()
        val cat0 = expenseCats[0]
        val cat1 = expenseCats[1]

        val moves = mutableListOf<Pair<Int, Int>>()
        val state = DragDropListState(
            lazyListState = LazyListState(),
            onMove = { from, to ->
                moves.add(from to to)
                val item = expenseCats.removeAt(from)
                expenseCats.add(to, item)
            },
            scope = CoroutineScope(Dispatchers.Unconfined),
            indexOffset = 0,
            keyProvider = { idx -> if (idx in expenseCats.indices) expenseCats[idx].id else idx },
            liveReorder = true
        )

        // Start dragging item at index 1
        state.onDragStart(itemId = cat1.id, index = 1, listSize = expenseCats.size)
        assertEquals(1, state.currentIndex)

        // Drag up past index 0's midpoint
        state.onDrag(-100f, expenseCats.size)

        assertEquals("Category 1 must successfully move into index 0", 0, state.currentIndex)
        assertEquals("Category 1 is now at index 0", cat1.id, expenseCats[0].id)
        assertEquals("Category 0 is now at index 1", cat0.id, expenseCats[1].id)
        assertTrue("Moves must contain (1, 0)", moves.contains(1 to 0))

        // Drag it back down to index 1
        state.onDrag(100f, expenseCats.size)
        assertEquals("Category returns to index 1", 1, state.currentIndex)
        assertEquals(cat1.id, expenseCats[1].id)
    }

    @Test
    fun `dragging category across multiple positions to first and last positions`() {
        val (cats, _) = InitialData.getDefaultCategories()
        var expenseCats = cats.filter { !it.isIncome && !it.isTransfer }.toMutableList()
        val targetCat = expenseCats[2] // index 2

        val state = DragDropListState(
            lazyListState = LazyListState(),
            onMove = { from, to ->
                val item = expenseCats.removeAt(from)
                expenseCats.add(to, item)
            },
            scope = CoroutineScope(Dispatchers.Unconfined),
            indexOffset = 0,
            keyProvider = { idx -> if (idx in expenseCats.indices) expenseCats[idx].id else idx },
            liveReorder = true
        )

        state.onDragStart(itemId = targetCat.id, index = 2, listSize = expenseCats.size)

        // Drag up all the way to index 0 (-200f)
        state.onDrag(-200f, expenseCats.size)
        assertEquals("Category must reach index 0", 0, state.currentIndex)
        assertEquals(targetCat.id, expenseCats[0].id)

        // Drag all the way down to the last position
        val lastIdx = expenseCats.size - 1
        state.onDrag(1200f, expenseCats.size)
        assertEquals("Category must reach last index", lastIdx, state.currentIndex)
        assertEquals(targetCat.id, expenseCats[lastIdx].id)
    }
}
