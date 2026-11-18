package com.example

import android.app.Application
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.test.core.app.ApplicationProvider
import com.example.ui.DEFAULT_HOME_SECTIONS
import com.example.ui.MainViewModel
import com.example.ui.BalanceCardStyle
import com.example.ui.HomeCardStyle
import com.example.ui.HomeCustomization
import com.example.ui.util.DragDropListState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CustomizeHomeDragDropTest {

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(
                context,
                FirebaseOptions.Builder()
                    .setApplicationId("com.example.app")
                    .setProjectId("test-project")
                    .setApiKey("fake-api-key")
                    .build()
            )
        }
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
        override val viewportEndOffset: Int = 1000,
        override val totalItemsCount: Int = visibleItemsInfo.size
    ) : LazyListLayoutInfo {
        override val afterContentPadding: Int = 0
        override val beforeContentPadding: Int = 0
        override val orientation = androidx.compose.foundation.gestures.Orientation.Vertical
        override val reverseLayout: Boolean = false
        override val viewportSize = androidx.compose.ui.unit.IntSize(400, 1000)
    }

    private fun createMockLazyListState(items: List<String>, itemHeight: Int = 70): LazyListState {
        val state = LazyListState(0, 0)
        return state
    }

    @Test
    fun `initial state is idle and clean`() {
        val state = DragDropListState(
            lazyListState = LazyListState(),
            onMove = { _, _ -> },
            scope = CoroutineScope(Dispatchers.Unconfined)
        )

        assertFalse(state.isDragging)
        assertNull(state.draggedItemId)
        assertEquals(-1, state.originalIndex)
        assertEquals(-1, state.currentIndex)
        assertEquals(0f, state.dragOffsetY, 0.001f)
    }

    @Test
    fun `drag start initializes state correctly with stable ID and index`() {
        val state = DragDropListState(
            lazyListState = LazyListState(),
            onMove = { _, _ -> },
            scope = CoroutineScope(Dispatchers.Unconfined)
        )

        state.onDragStart(itemId = "sec_reorder_smart_insight", index = 2, listSize = 10)

        assertTrue(state.isDragging)
        assertEquals("sec_reorder_smart_insight", state.draggedItemId)
        assertEquals(2, state.originalIndex)
        assertEquals(2, state.currentIndex)
        assertEquals(2, state.targetIndex)
        assertEquals(0f, state.dragOffsetY, 0.001f)
    }

    @Test
    fun `slow drag down crosses item midpoint and swaps correctly`() {
        val list = mutableListOf("budget", "bills", "recent_activity", "savings", "financial_health")
        var movedFrom = -1
        var movedTo = -1

        val state = DragDropListState(
            lazyListState = LazyListState(),
            onMove = { from, to ->
                movedFrom = from
                movedTo = to
                val item = list.removeAt(from)
                list.add(to, item)
            },
            scope = CoroutineScope(Dispatchers.Unconfined),
            keyProvider = { idx -> "sec_reorder_${list[idx]}" },
            liveReorder = true
        )

        state.onDragStart("sec_reorder_bills", 1, list.size)
        assertEquals(1, state.currentIndex)

        // Drag down by 80px (crossing the next item's midpoint threshold)
        state.onDrag(dragAmountY = 80f, listSize = list.size)

        // Should have swapped index 1 with index 2
        assertEquals(1, movedFrom)
        assertEquals(2, movedTo)
        assertEquals(2, state.currentIndex)
        assertEquals(listOf("budget", "recent_activity", "bills", "savings", "financial_health"), list)
    }

    @Test
    fun `safeguard against oscillation prevents repeated swaps while hovering over target`() {
        val list = mutableListOf("budget", "bills", "recent_activity", "savings", "financial_health")
        var swapCount = 0

        val state = DragDropListState(
            lazyListState = LazyListState(),
            onMove = { from, to ->
                swapCount++
                val item = list.removeAt(from)
                list.add(to, item)
            },
            scope = CoroutineScope(Dispatchers.Unconfined),
            keyProvider = { idx -> "sec_reorder_${list[idx]}" },
            liveReorder = true
        )

        // Start drag on "bills" at index 1
        state.onDragStart("sec_reorder_bills", 1, list.size)

        // Move down by 80px to cross into index 2
        state.onDrag(dragAmountY = 80f, listSize = list.size)
        assertEquals(1, swapCount)
        assertEquals(2, state.currentIndex)

        // User holds finger steady (subsequent drag movements of 0px or small jitter +1px, -1px)
        state.onDrag(dragAmountY = 1f, listSize = list.size)
        state.onDrag(dragAmountY = -1f, listSize = list.size)
        state.onDrag(dragAmountY = 0f, listSize = list.size)

        // Safeguard holds: NO further swaps should occur while hovering over the target!
        assertEquals(1, swapCount)
        assertEquals(2, state.currentIndex)
        assertEquals("bills", list[2])
    }

    @Test
    fun `fast drag across multiple rows downwards reorders accurately`() {
        val list = mutableListOf("A", "B", "C", "D", "E")
        val state = DragDropListState(
            lazyListState = LazyListState(),
            onMove = { from, to ->
                val item = list.removeAt(from)
                list.add(to, item)
            },
            scope = CoroutineScope(Dispatchers.Unconfined),
            keyProvider = { idx -> "sec_${list[idx]}" },
            liveReorder = true
        )

        state.onDragStart("sec_A", 0, list.size)

        // Fast swipe down 300px
        state.onDrag(dragAmountY = 300f, listSize = list.size)

        // Should have moved past B, C, D to near bottom
        assertTrue(state.currentIndex >= 2)
        assertEquals("A", list[state.currentIndex])
    }

    @Test
    fun `fast drag upwards across multiple rows reorders accurately`() {
        val list = mutableListOf("A", "B", "C", "D", "E")
        val state = DragDropListState(
            lazyListState = LazyListState(),
            onMove = { from, to ->
                val item = list.removeAt(from)
                list.add(to, item)
            },
            scope = CoroutineScope(Dispatchers.Unconfined),
            keyProvider = { idx -> "sec_${list[idx]}" },
            liveReorder = true
        )

        // Start drag on "E" at index 4
        state.onDragStart("sec_E", 4, list.size)

        // Fast swipe up -300px
        state.onDrag(dragAmountY = -300f, listSize = list.size)

        // Should have moved upwards towards index 0
        assertTrue(state.currentIndex <= 2)
        assertEquals("E", list[state.currentIndex])
    }

    @Test
    fun `cancellation safety restores original list order`() {
        val initialList = listOf("budget", "bills", "recent_activity", "savings", "financial_health")
        val list = initialList.toMutableList()

        val state = DragDropListState(
            lazyListState = LazyListState(),
            onMove = { from, to ->
                val item = list.removeAt(from)
                list.add(to, item)
            },
            scope = CoroutineScope(Dispatchers.Unconfined),
            keyProvider = { idx -> "sec_${list[idx]}" },
            liveReorder = true
        )

        state.onDragStart("sec_budget", 0, list.size)
        state.onDrag(100f, list.size) // reorders to index 1
        assertEquals(1, state.currentIndex)

        // Cancel drag
        state.onDragCancel()

        // List must be restored to original order
        assertEquals(initialList, list)
        assertFalse(state.isDragging)
        assertNull(state.draggedItemId)
    }

    @Test
    fun `drop completion commits final order to preferences and persists across reload`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm1 = MainViewModel(app)

        val customOrder = listOf(
            "shortcuts",
            "smart_insights",
            "spending_streak",
            "top_spending",
            "budget",
            "bills",
            "recent_activity",
            "savings",
            "financial_health",
            "spending_by_day"
        )

        val updated = HomeCustomization(
            cardStyle = HomeCardStyle.STACK,
            balanceCardStyle = BalanceCardStyle.STANDARD,
            showBudgetProgress = true,
            showUpcomingBills = true,
            showRecentActivity = true,
            showSavingsGoals = true,
            showFinancialHealth = false,
            showSmartInsights = true,
            showTopSpending = false,
            showSpendingByDay = false,
            showSpendingStreak = true,
            showQuickShortcuts = true,
            sectionOrder = customOrder
        )

        // Commit customization
        vm1.updateHomeCustomization(updated)
        assertEquals(customOrder, vm1.homeCustomization.value.sectionOrder)

        // Simulate reopening Customize Home / recreating ViewModel from SharedPreferences
        val vm2 = MainViewModel(app)
        val loadedCustomization = vm2.homeCustomization.value

        assertEquals(customOrder, loadedCustomization.sectionOrder)
        assertTrue(loadedCustomization.showSmartInsights)
        assertTrue(loadedCustomization.showSpendingStreak)
        assertTrue(loadedCustomization.showQuickShortcuts)
    }

    @Test
    fun `reset restores DEFAULT_HOME_SECTIONS order in preferences`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = MainViewModel(app)

        val customOrder = listOf("shortcuts", "smart_insights", "budget", "bills", "recent_activity", "savings", "financial_health", "top_spending", "spending_by_day", "spending_streak")
        vm.updateHomeCustomization(vm.homeCustomization.value.copy(sectionOrder = customOrder))
        assertEquals(customOrder, vm.homeCustomization.value.sectionOrder)

        // Now perform reset
        val resetCustomization = HomeCustomization(
            cardStyle = HomeCardStyle.STACK,
            balanceCardStyle = BalanceCardStyle.STANDARD,
            showBudgetProgress = true,
            showUpcomingBills = true,
            showRecentActivity = true,
            showSavingsGoals = false,
            showFinancialHealth = false,
            showSmartInsights = false,
            showTopSpending = false,
            showSpendingByDay = false,
            showSpendingStreak = false,
            showQuickShortcuts = false,
            sectionOrder = DEFAULT_HOME_SECTIONS
        )
        vm.updateHomeCustomization(resetCustomization)

        assertEquals(DEFAULT_HOME_SECTIONS, vm.homeCustomization.value.sectionOrder)

        // Reopen verification
        val vmReloaded = MainViewModel(app)
        assertEquals(DEFAULT_HOME_SECTIONS, vmReloaded.homeCustomization.value.sectionOrder)
    }

    @Test
    fun `dragging topmost section with indexOffset 1 calculates initialItemOffset and translationY without upward jump`() {
        val list = mutableListOf("budget", "bills", "recent_activity", "savings", "financial_health")
        val state = DragDropListState(
            lazyListState = LazyListState(),
            onMove = { from, to ->
                val item = list.removeAt(from)
                list.add(to, item)
            },
            scope = CoroutineScope(Dispatchers.Unconfined),
            indexOffset = 1,
            keyProvider = { idx -> "sec_reorder_${list[idx]}" },
            liveReorder = true
        )

        // Drag starts on topmost section (index 0)
        state.onDragStart("sec_reorder_budget", 0, list.size)
        assertTrue(state.isDragging)
        assertEquals(0, state.currentIndex)
        assertEquals("sec_reorder_budget", state.draggedItemId)

        // Translation starts at exactly 0, zero jump
        assertEquals(0f, state.translationY, 0.001f)

        // Drag down by 75px (crossing into index 1)
        state.onDrag(dragAmountY = 75f, listSize = list.size)
        assertEquals(1, state.currentIndex)
        assertEquals("budget", list[1])
        assertEquals("bills", list[0])

        // Continue dragging down several positions (+160px more)
        state.onDrag(dragAmountY = 160f, listSize = list.size)
        assertTrue(state.currentIndex >= 2)
        assertEquals("budget", list[state.currentIndex])

        // Drag back upward towards the top (-240px)
        state.onDrag(dragAmountY = -240f, listSize = list.size)
        assertEquals(0, state.currentIndex)
        assertEquals("budget", list[0])

        // Drop ends cleanly and commits
        state.onDragEnd()
        assertFalse(state.isDragging)
        assertNull(state.draggedItemId)
    }

    @Test
    fun `dragging across multiple items down and back up preserves list integrity`() {
        val list = mutableListOf("1", "2", "3", "4", "5")
        val state = DragDropListState(
            lazyListState = LazyListState(),
            onMove = { from, to ->
                val item = list.removeAt(from)
                list.add(to, item)
            },
            scope = CoroutineScope(Dispatchers.Unconfined),
            indexOffset = 1,
            keyProvider = { idx -> "sec_${list[idx]}" },
            liveReorder = true
        )

        // Drag index 0 all the way to index 4
        state.onDragStart("sec_1", 0, list.size)
        state.onDrag(dragAmountY = 320f, listSize = list.size)
        assertEquals(4, state.currentIndex)
        assertEquals("1", list[4])

        // Drag back to index 2
        state.onDrag(dragAmountY = -140f, listSize = list.size)
        assertTrue(state.currentIndex in 1..3)
        assertEquals("1", list[state.currentIndex])

        state.onDragEnd()
        assertFalse(state.isDragging)
    }
}

