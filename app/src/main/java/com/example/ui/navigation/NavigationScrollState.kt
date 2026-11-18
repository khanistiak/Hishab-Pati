package com.example.ui.navigation

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import com.example.ui.MainTab
import com.example.ui.MainViewModel
import com.example.ui.SubScreen
import kotlinx.coroutines.flow.distinctUntilChanged
import java.util.concurrent.ConcurrentHashMap

data class LazyScrollPosition(
    val index: Int = 0,
    val offset: Int = 0
)

data class SubScreenNavEntry(
    val screen: SubScreen,
    val instanceKey: String = "root",
    val entryId: Long
) {
    val stateKey: String
        get() = "SUB:${entryId}:${screen.name}:${instanceKey}"
}

class NavigationScrollStateStore {
    private val lazyPositions = ConcurrentHashMap<String, LazyScrollPosition>()
    private val scrollPositions = ConcurrentHashMap<String, Int>()
    private val entryUiStates = ConcurrentHashMap<String, Any>()

    fun getLazyPosition(key: String): LazyScrollPosition {
        return lazyPositions[key] ?: LazyScrollPosition(0, 0)
    }

    fun saveLazyPosition(key: String, index: Int, offset: Int) {
        lazyPositions[key] = LazyScrollPosition(
            index = index.coerceAtLeast(0),
            offset = offset.coerceAtLeast(0)
        )
    }

    fun getScrollPosition(key: String): Int {
        return scrollPositions[key] ?: 0
    }

    fun saveScrollPosition(key: String, value: Int) {
        scrollPositions[key] = value.coerceAtLeast(0)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getUiState(key: String): T? {
        return entryUiStates[key] as? T
    }

    fun saveUiState(key: String, value: Any?) {
        if (value == null) {
            entryUiStates.remove(key)
        } else {
            entryUiStates[key] = value
        }
    }

    fun clearScopePrefix(prefix: String) {
        val fullPrefix = "$prefix|"
        lazyPositions.keys.removeIf { it == prefix || it.startsWith(fullPrefix) }
        scrollPositions.keys.removeIf { it == prefix || it.startsWith(fullPrefix) }
        entryUiStates.keys.removeIf { it == prefix || it.startsWith(fullPrefix) }
    }

    fun clearAllSubScreens() {
        lazyPositions.keys.removeIf { it.startsWith("SUB:") }
        scrollPositions.keys.removeIf { it.startsWith("SUB:") }
        entryUiStates.keys.removeIf { it.startsWith("SUB:") }
    }
}

@Composable
fun rememberNavigationScopeKey(
    viewModel: MainViewModel,
    screenOverride: SubScreen? = null,
    tabOverride: MainTab? = null
): String {
    val currentTab by viewModel.currentTab.collectAsState()
    val currentEntry by viewModel.currentSubScreenEntry.collectAsState()
    return viewModel.resolveNavigationScopeKey(
        screenOverride = screenOverride,
        tabOverride = tabOverride,
        activeTab = currentTab,
        activeEntry = currentEntry
    )
}

@Composable
fun rememberScreenLazyListState(
    viewModel: MainViewModel,
    subKey: String = "main",
    screenOverride: SubScreen? = null,
    tabOverride: MainTab? = null
): LazyListState {
    val scopeKey = rememberNavigationScopeKey(
        viewModel = viewModel,
        screenOverride = screenOverride,
        tabOverride = tabOverride
    )
    val fullKey = "$scopeKey|LAZY:$subKey"

    return key(fullKey) {
        val initial = viewModel.scrollStateStore.getLazyPosition(fullKey)
        val state = rememberSaveable(
            fullKey,
            saver = LazyListState.Saver
        ) {
            LazyListState(
                firstVisibleItemIndex = initial.index,
                firstVisibleItemScrollOffset = initial.offset
            )
        }

        LaunchedEffect(state, fullKey) {
            val saved = viewModel.scrollStateStore.getLazyPosition(fullKey)
            if ((saved.index > 0 || saved.offset > 0) &&
                state.firstVisibleItemIndex == 0 &&
                state.firstVisibleItemScrollOffset == 0
            ) {
                state.scrollToItem(saved.index, saved.offset)
            }
            snapshotFlow {
                state.firstVisibleItemIndex to state.firstVisibleItemScrollOffset
            }
                .distinctUntilChanged()
                .collect { (index, offset) ->
                    viewModel.scrollStateStore.saveLazyPosition(fullKey, index, offset)
                    if (screenOverride == SubScreen.SETTINGS && subKey == "main") {
                        viewModel.updateSettingsScrollPosition(index, offset)
                    }
                }
        }

        DisposableEffect(state, fullKey) {
            onDispose {
                val index = state.firstVisibleItemIndex
                val offset = state.firstVisibleItemScrollOffset
                viewModel.scrollStateStore.saveLazyPosition(fullKey, index, offset)
                if (screenOverride == SubScreen.SETTINGS && subKey == "main") {
                    viewModel.updateSettingsScrollPosition(index, offset)
                }
            }
        }

        state
    }
}

@Composable
fun rememberScreenScrollState(
    viewModel: MainViewModel,
    subKey: String = "main",
    screenOverride: SubScreen? = null,
    tabOverride: MainTab? = null
): ScrollState {
    val scopeKey = rememberNavigationScopeKey(
        viewModel = viewModel,
        screenOverride = screenOverride,
        tabOverride = tabOverride
    )
    val fullKey = "$scopeKey|SCROLL:$subKey"

    return key(fullKey) {
        val initial = viewModel.scrollStateStore.getScrollPosition(fullKey)
        val state = rememberSaveable(
            fullKey,
            saver = ScrollState.Saver
        ) {
            ScrollState(initial = initial)
        }

        LaunchedEffect(state, fullKey) {
            val saved = viewModel.scrollStateStore.getScrollPosition(fullKey)
            if (saved > 0 && state.value == 0) {
                state.scrollTo(saved)
            }
            snapshotFlow { state.value }
                .distinctUntilChanged()
                .collect { value ->
                    viewModel.scrollStateStore.saveScrollPosition(fullKey, value)
                }
        }

        DisposableEffect(state, fullKey) {
            onDispose {
                viewModel.scrollStateStore.saveScrollPosition(fullKey, state.value)
            }
        }

        state
    }
}
