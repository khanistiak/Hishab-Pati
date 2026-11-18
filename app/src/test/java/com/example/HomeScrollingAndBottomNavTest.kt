package com.example

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.dp
import com.example.data.model.AppLanguage
import com.example.data.model.ThemeMode
import com.example.ui.MainTab
import com.example.ui.components.FrostedBackdropState
import com.example.ui.components.LocalFrostedBackdropState
import com.example.ui.components.ScrollAwareFixedHeader
import com.example.ui.components.recordFrostedBackdrop
import com.example.ui.components.rememberScrollAwareGlassLayout
import com.example.ui.theme.MyWalletBackground
import com.example.ui.theme.MyWalletTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HomeScrollingAndBottomNavTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun bottomNavigation_placesFourTabsInCapsule_movesLiquidIndicator_andAlignsPlusAction() {
        var selectedTab = MainTab.HOME
        var addClickCount = 0

        composeTestRule.setContent {
            var tabState by remember { mutableStateOf(MainTab.HOME) }
            val graphicsLayer = rememberGraphicsLayer()
            val bgLayer = rememberGraphicsLayer()
            val backdropState = remember(graphicsLayer, bgLayer) {
                FrostedBackdropState(graphicsLayer, bgLayer)
            }

            MyWalletTheme(themeMode = ThemeMode.LIGHT) {
                CompositionLocalProvider(LocalFrostedBackdropState provides backdropState) {
                    MyWalletBottomNavigationBar(
                        currentTab = tabState,
                        language = AppLanguage.ENGLISH,
                        onTabSelected = {
                            tabState = it
                            selectedTab = it
                        },
                        onAddClick = {
                            addClickCount++
                        }
                    )
                }
            }
        }

        val capsuleNode = composeTestRule.onNodeWithTag("bottom_nav_capsule").assertIsDisplayed()
        val addBtnNode = composeTestRule.onNodeWithTag("floating_add_transaction_button").assertIsDisplayed()
        val indicatorNode = composeTestRule.onNodeWithTag("bottom_nav_selected_indicator").assertIsDisplayed()

        val homeNode = composeTestRule.onNodeWithTag("tab_home").assertIsDisplayed()
        val activityNode = composeTestRule.onNodeWithTag("tab_activity").assertIsDisplayed()
        val planNode = composeTestRule.onNodeWithTag("tab_plan").assertIsDisplayed()
        val insightsNode = composeTestRule.onNodeWithTag("tab_insights").assertIsDisplayed()

        val capsuleBounds = capsuleNode.fetchSemanticsNode().boundsInRoot
        val addBtnBounds = addBtnNode.fetchSemanticsNode().boundsInRoot
        val initialIndicatorBounds = indicatorNode.fetchSemanticsNode().boundsInRoot
        val homeBounds = homeNode.fetchSemanticsNode().boundsInRoot

        // 1. All 4 tabs must be inside the rounded capsule bounds horizontally
        for (tabNode in listOf(homeNode, activityNode, planNode, insightsNode)) {
            val tabBounds = tabNode.fetchSemanticsNode().boundsInRoot
            assertTrue(
                "Tab should be horizontally inside bottom_nav_capsule",
                tabBounds.left >= capsuleBounds.left && tabBounds.right <= capsuleBounds.right
            )
        }

        // Initial indicator must be aligned with Home tab
        assertTrue(
            "Selected indicator should initially align with Home tab",
            abs(initialIndicatorBounds.center.x - homeBounds.center.x) < 4f
        )

        // 2. Circular '+' button must be positioned to the right of the capsule (outside it)
        assertTrue(
            "Circular '+' button must be positioned beside (to the right of) the capsule",
            addBtnBounds.left >= capsuleBounds.right
        )

        // 3. Capsule and '+' button must be aligned to the same height and vertical center
        assertTrue(
            "Capsule and '+' button should have matching height",
            abs(capsuleBounds.height - addBtnBounds.height) < 2f
        )
        assertTrue(
            "Capsule and '+' button should be vertically aligned",
            abs(capsuleBounds.center.y - addBtnBounds.center.y) < 2f
        )

        // 4. Switching tabs smoothly moves the liquid-crystal indicator to each selected tab
        activityNode.performClick()
        composeTestRule.waitForIdle()
        assertEquals(MainTab.ACTIVITY, selectedTab)
        val activityIndicatorBounds = indicatorNode.fetchSemanticsNode().boundsInRoot
        val activityBounds = activityNode.fetchSemanticsNode().boundsInRoot
        assertTrue(
            "Selected indicator should move to Activity tab",
            abs(activityIndicatorBounds.center.x - activityBounds.center.x) < 4f
        )

        planNode.performClick()
        composeTestRule.waitForIdle()
        assertEquals(MainTab.PLAN, selectedTab)

        insightsNode.performClick()
        composeTestRule.waitForIdle()
        assertEquals(MainTab.INSIGHTS, selectedTab)
        val insightsIndicatorBounds = indicatorNode.fetchSemanticsNode().boundsInRoot
        val insightsBounds = insightsNode.fetchSemanticsNode().boundsInRoot
        assertTrue(
            "Selected indicator should move to Insights tab",
            abs(insightsIndicatorBounds.center.x - insightsBounds.center.x) < 4f
        )

        addBtnNode.performClick()
        assertEquals(1, addClickCount)
    }

    @Test
    fun sharedScrollAwareFixedHeader_clearAtTopAndStrengthensOnScrollAcrossTabs() {
        var recordedHolder: FrostedBackdropState? = null

        composeTestRule.setContent {
            MyWalletTheme(themeMode = ThemeMode.LIGHT) {
                MyWalletBackground(isDark = false) {
                    val listState = rememberLazyListState()
                    val scrollLayout = rememberScrollAwareGlassLayout(
                        listState = listState,
                        defaultHeaderHeight = 72.dp
                    )
                    recordedHolder = scrollLayout.backdropState

                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .recordFrostedBackdrop(scrollLayout.backdropState, listState)
                                .testTag("scrolling_list"),
                            contentPadding = scrollLayout.contentPadding
                        ) {
                            items(30) { index ->
                                Text(
                                    text = "Transaction Row $index",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                )
                            }
                        }

                        ScrollAwareFixedHeader(
                            layoutState = scrollLayout,
                            testTag = "fixed_frosted_header"
                        ) {
                            Text(
                                text = "Fixed Header",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(72.dp)
                            )
                        }
                    }
                }
            }
        }

        composeTestRule.onNodeWithTag("fixed_frosted_header").assertIsDisplayed()
        composeTestRule.onNodeWithTag("scrolling_list").assertIsDisplayed()
        composeTestRule.waitForIdle()

        // 1. At top and not scrolling: targetGlassProgress must be 0f (fully clear/transparent)
        assertEquals(0f, recordedHolder?.targetGlassProgress ?: -1f, 0.001f)
        assertTrue(
            "Both background and scrolling content layers should be positioned",
            recordedHolder?.hasRecordedContent == true && recordedHolder?.hasRecordedBackground == true
        )

        // 2. Scroll down: targetGlassProgress increases to enable subtle liquid-crystal glass
        composeTestRule.onNodeWithTag("scrolling_list").performScrollToIndex(5)
        composeTestRule.waitForIdle()
        assertTrue(
            "Scrolling down should increase targetGlassProgress above 0f",
            (recordedHolder?.targetGlassProgress ?: 0f) > 0.5f
        )

        // 3. Scroll back to the top: targetGlassProgress returns to 0f (fully clear state)
        composeTestRule.onNodeWithTag("scrolling_list").performScrollToIndex(0)
        composeTestRule.waitForIdle()
        assertEquals(0f, recordedHolder?.targetGlassProgress ?: -1f, 0.001f)
    }

    @Test
    fun tabSwitching_isImmediateWithoutPreviousTabFrame_andPreservesEachTabsScrollPosition() {
        var homeFirstVisibleIndex = 0
        var activityFirstVisibleIndex = 0
        var backdropHolder: FrostedBackdropState? = null

        composeTestRule.setContent {
            var currentTab by remember { mutableStateOf(MainTab.HOME) }
            val homeListState = rememberLazyListState()
            val activityListState = rememberLazyListState()
            val planListState = rememberLazyListState()
            val insightsListState = rememberLazyListState()
            val stateHolder = rememberSaveableStateHolder()

            homeFirstVisibleIndex = homeListState.firstVisibleItemIndex
            activityFirstVisibleIndex = activityListState.firstVisibleItemIndex

            MyWalletTheme(themeMode = ThemeMode.LIGHT) {
                MyWalletBackground(isDark = false) {
                    backdropHolder = LocalFrostedBackdropState.current
                    Box(modifier = Modifier.fillMaxSize()) {
                        key(currentTab) {
                            stateHolder.SaveableStateProvider(currentTab.name) {
                                val activeListState = when (currentTab) {
                                    MainTab.HOME -> homeListState
                                    MainTab.ACTIVITY -> activityListState
                                    MainTab.PLAN -> planListState
                                    MainTab.INSIGHTS -> insightsListState
                                }
                                val scrollLayout = rememberScrollAwareGlassLayout(listState = activeListState)
                                Box(modifier = Modifier.fillMaxSize()) {
                                    LazyColumn(
                                        state = activeListState,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .recordFrostedBackdrop(
                                                scrollLayout.backdropState,
                                                activeListState,
                                                scrollLayout.contentGraphicsLayer
                                            )
                                            .testTag("list_${currentTab.name}"),
                                        contentPadding = scrollLayout.contentPadding
                                    ) {
                                        items(40) { idx ->
                                            Text(
                                                text = "${currentTab.name} Item $idx",
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(60.dp)
                                            )
                                        }
                                    }
                                    ScrollAwareFixedHeader(
                                        layoutState = scrollLayout,
                                        testTag = "header_${currentTab.name}"
                                    ) {
                                        Text(
                                            text = "Header ${currentTab.name}",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(64.dp)
                                        )
                                    }
                                }
                            }
                        }

                        MyWalletBottomNavigationBar(
                            currentTab = currentTab,
                            language = AppLanguage.ENGLISH,
                            onTabSelected = { currentTab = it },
                            onAddClick = {}
                        )
                    }
                }
            }
        }

        // Scroll Home tab down to index 8
        composeTestRule.onNodeWithTag("list_HOME").performScrollToIndex(8)
        composeTestRule.waitForIdle()
        assertEquals(8, homeFirstVisibleIndex)
        val homeActiveLayer: GraphicsLayer? = backdropHolder?.activeContentLayer

        // Step-by-step clock check: selecting Activity tab must switch on the very first frame
        // with zero frames of the previous Home tab remaining in the tree
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.onNodeWithTag("tab_activity").performClick()
        composeTestRule.mainClock.advanceTimeByFrame()

        composeTestRule.onNodeWithTag("list_HOME").assertDoesNotExist()
        composeTestRule.onNodeWithTag("header_HOME").assertDoesNotExist()
        composeTestRule.onNodeWithTag("header_ACTIVITY").assertIsDisplayed()
        composeTestRule.onNodeWithTag("list_ACTIVITY").assertIsDisplayed()

        val activityActiveLayer: GraphicsLayer? = backdropHolder?.activeContentLayer
        assertNotSame(
            "Destination tab must use its own isolated GraphicsLayer so previous tab pixels never flash",
            homeActiveLayer,
            activityActiveLayer
        )

        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.waitForIdle()

        // Scroll Activity tab to index 12
        composeTestRule.onNodeWithTag("list_ACTIVITY").performScrollToIndex(12)
        composeTestRule.waitForIdle()
        assertEquals(12, activityFirstVisibleIndex)

        // Switch to Plan tab, then return to Home tab -> Home scroll position must still be 8
        composeTestRule.onNodeWithTag("tab_plan").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("header_PLAN").assertIsDisplayed()

        composeTestRule.onNodeWithTag("tab_home").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("header_HOME").assertIsDisplayed()
        assertEquals(8, homeFirstVisibleIndex)

        // Switch back to Activity tab -> Activity scroll position must still be 12
        composeTestRule.onNodeWithTag("tab_activity").performClick()
        composeTestRule.waitForIdle()
        assertEquals(12, activityFirstVisibleIndex)
    }

    @Test
    fun theme_filteredReports_andSettings_haveFixedFrostedGlassHeaders() {
        var recordedHolder: FrostedBackdropState? = null

        composeTestRule.setContent {
            MyWalletTheme(themeMode = ThemeMode.LIGHT) {
                MyWalletBackground(isDark = false) {
                    val listState = rememberLazyListState()
                    val scrollLayout = rememberScrollAwareGlassLayout(
                        listState = listState,
                        defaultHeaderHeight = 68.dp,
                        extraTopGap = 8.dp,
                        bottomNavClearance = 32.dp
                    )
                    recordedHolder = scrollLayout.backdropState

                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .recordFrostedBackdrop(scrollLayout.backdropState, listState)
                                .testTag("screen_lazy_column"),
                            contentPadding = scrollLayout.contentPadding
                        ) {
                            items(40) { index ->
                                Text(
                                    text = "Scrollable Content Item $index",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                )
                            }
                        }

                        ScrollAwareFixedHeader(
                            layoutState = scrollLayout,
                            testTag = "screen_fixed_header"
                        ) {
                            Text("Fixed Screen Header", modifier = Modifier.height(48.dp))
                        }
                    }
                }
            }
        }

        composeTestRule.onNodeWithTag("screen_fixed_header").assertIsDisplayed()
        composeTestRule.onNodeWithTag("screen_lazy_column").assertIsDisplayed()
        composeTestRule.waitForIdle()

        // 1. Initial state at top of page: glass progress must be clear (0f)
        assertEquals(0f, recordedHolder?.targetGlassProgress ?: -1f, 0.001f)

        // 2. Scroll content down: glass progress increases to smoothly blur content underneath
        composeTestRule.onNodeWithTag("screen_lazy_column").performScrollToIndex(6)
        composeTestRule.waitForIdle()
        assertTrue(
            "Progress should be greater than 0f when scrolled",
            (recordedHolder?.targetGlassProgress ?: 0f) > 0f
        )
    }
}

