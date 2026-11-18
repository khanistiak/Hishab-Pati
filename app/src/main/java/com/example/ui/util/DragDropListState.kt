package com.example.ui.util

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Robust, production-grade state holder and gesture coordinator for LazyColumn drag-and-drop reordering.
 *
 * Guarantees:
 * 1. Maintain one stable drag state containing the dragged item ID, original index, current index, and pointer offset.
 * 2. Visual stability: [translationY] continuously equals (clampedFloatingItemTop - currentSlotOffset), so
 *    the dragged item remains rigidly and smoothly attached to the user's finger with ZERO jumping or blinking.
 * 3. Surrounding rows animate smoothly using Compose's placement animations ([Modifier.animateItem]), while the
 *    dragged row does NOT animate its placement, preventing animation fight/flicker.
 * 4. Midpoint crossing threshold with natural half-row hysteresis buffer to prevent index oscillation and repeated swaps.
 * 5. Multi-step reordering support for fast dragging across multiple items in both directions.
 * 6. Edge auto-scrolling synchronized with viewport bounds, reordering items as they scroll beneath the finger.
 * 7. Dropping commits the final order; cancellation safely restores the initial order.
 */
class DragDropListState(
    val lazyListState: LazyListState,
    var onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    var onDragEnd: () -> Unit = {},
    var onDragCancel: () -> Unit = {},
    private val scope: CoroutineScope,
    var indexOffset: Int = 0,
    var keyProvider: ((index: Int) -> Any)? = null,
    val liveReorder: Boolean = true
) {
    // Stable identifier for the currently dragged item (e.g. key or ID)
    var draggedItemId by mutableStateOf<Any?>(null)
        private set

    var originalIndex by mutableIntStateOf(-1)
        private set

    var currentIndex by mutableIntStateOf(-1)
        private set

    var targetIndex by mutableIntStateOf(-1)
        private set

    var isDragging by mutableStateOf(false)
        private set

    var dragOffsetY by mutableFloatStateOf(0f)
        private set

    var initialItemOffset by mutableFloatStateOf(0f)
        private set

    var itemHeight by mutableFloatStateOf(160f)
        private set

    private var lastKnownSlotOffset by mutableFloatStateOf(0f)

    /** Backward-compatible accessor for existing screens */
    val draggedIndex: Int?
        get() = if (isDragging) currentIndex else null

    private var autoScrollJob: Job? = null
    var currentListSize: Int = 0
        private set

    val floatingItemTop: Float
        get() = initialItemOffset + dragOffsetY

    val clampedFloatingItemTop: Float
        get() {
            val layoutInfo = lazyListState.layoutInfo
            val viewportEnd = layoutInfo.viewportEndOffset.toFloat()
            if (viewportEnd <= 0f) {
                return floatingItemTop
            }
            // Allow elastic bounds so dragged row smoothly follows finger near list extremities
            val minOffset = -itemHeight * 0.4f
            val maxOffset = (viewportEnd - itemHeight * 0.6f).coerceAtLeast(minOffset)
            return floatingItemTop.coerceIn(minOffset, maxOffset)
        }

    val floatingCenterY: Float
        get() = floatingItemTop + itemHeight / 2f

    /**
     * Continuous visual translation for the dragged item:
     * `clampedFloatingItemTop - currentSlotOffset`
     *
     * In every frame (regardless of slot changes, layout passes, or animations),
     * `currentSlotOffset + translationY` strictly equals `clampedFloatingItemTop`.
     * This completely prevents visual jumps, blinking, and 1-frame glitches.
     */
    val translationY: Float
        get() {
            if (!isDragging) return 0f
            val key = draggedItemId
            val visibleItems = lazyListState.layoutInfo.visibleItemsInfo
            val currentInfo = (if (key != null) visibleItems.firstOrNull { it.key == key } else null)
                ?: visibleItems.firstOrNull { it.index == (currentIndex + indexOffset) }
            if (currentInfo != null) {
                lastKnownSlotOffset = currentInfo.offset.toFloat()
            }
            val slotOffset = currentInfo?.offset?.toFloat() ?: lastKnownSlotOffset
            return clampedFloatingItemTop - slotOffset
        }

    fun onDragStart(itemId: Any, index: Int, listSize: Int) {
        if (index !in 0 until listSize || listSize <= 1) return
        currentListSize = listSize
        draggedItemId = itemId
        originalIndex = index
        currentIndex = index
        targetIndex = index
        dragOffsetY = 0f
        isDragging = true

        val targetLazyIndex = index + indexOffset
        val visibleItems = lazyListState.layoutInfo.visibleItemsInfo
        val itemInfo = visibleItems.firstOrNull { it.key == itemId }
            ?: visibleItems.firstOrNull { it.index == targetLazyIndex }

        if (itemInfo != null) {
            initialItemOffset = itemInfo.offset.toFloat()
            itemHeight = itemInfo.size.toFloat().coerceAtLeast(40f)
            lastKnownSlotOffset = initialItemOffset
        } else {
            val estimated = (index + indexOffset) * 68f
            initialItemOffset = estimated
            itemHeight = 68f
            lastKnownSlotOffset = estimated
        }

        startAutoScrollLoop()
    }

    /** Overload for index-only caller */
    fun onDragStart(index: Int, listSize: Int) {
        val key = keyProvider?.invoke(index) ?: index
        onDragStart(key, index, listSize)
    }

    fun onDrag(dragAmountY: Float, listSize: Int) {
        if (!isDragging) return
        currentListSize = listSize
        dragOffsetY += dragAmountY

        if (liveReorder) {
            checkAndPerformReorder(listSize)
        } else {
            updateTargetIndexStatic(listSize)
        }
    }

    /**
     * Checks if the dragged item's visual center has crossed an adjacent item's midpoint.
     * When crossed, swaps the items in the list and updates currentIndex.
     *
     * Natural hysteresis safeguard: Because the center must cross the other item's midpoint
     * (which is ~1 row away), moving across the threshold requires deliberate finger motion
     * in the opposite direction. It is impossible to oscillate or repeatedly swap while holding still.
     */
    fun checkAndPerformReorder(listSize: Int) {
        if (!isDragging || listSize <= 1) return
        val visibleItems = lazyListState.layoutInfo.visibleItemsInfo
        val center = floatingCenterY

        var hasMoved = true
        var iterations = 0
        while (hasMoved && iterations < listSize) {
            hasMoved = false
            iterations++
            val activeIdx = currentIndex
            if (activeIdx !in 0 until listSize) break

            // Check moving DOWN (towards listSize - 1)
            if (activeIdx < listSize - 1) {
                val nextLazyIndex = activeIdx + 1 + indexOffset
                val nextKey = keyProvider?.invoke(activeIdx + 1)
                val nextItemInfo = (if (nextKey != null) visibleItems.firstOrNull { it.key == nextKey } else null)
                    ?: visibleItems.firstOrNull { it.index == nextLazyIndex }

                val nextThreshold = if (nextItemInfo != null) {
                    nextItemInfo.offset + nextItemInfo.size / 2f
                } else {
                    val currInfo = (if (draggedItemId != null) visibleItems.firstOrNull { it.key == draggedItemId } else null)
                        ?: visibleItems.firstOrNull { it.index == activeIdx + indexOffset }
                    if (currInfo != null) {
                        currInfo.offset + currInfo.size + itemHeight / 2f
                    } else {
                        initialItemOffset + (activeIdx - originalIndex + 1) * itemHeight
                    }
                }

                if (nextThreshold != null && center > nextThreshold) {
                    val toIndex = activeIdx + 1
                    currentIndex = toIndex
                    targetIndex = toIndex
                    onMove(activeIdx, toIndex)
                    hasMoved = true
                    continue
                }
            }

            // Check moving UP (towards 0)
            if (activeIdx > 0) {
                val prevLazyIndex = activeIdx - 1 + indexOffset
                val prevKey = keyProvider?.invoke(activeIdx - 1)
                val prevItemInfo = (if (prevKey != null) visibleItems.firstOrNull { it.key == prevKey } else null)
                    ?: visibleItems.firstOrNull { it.index == prevLazyIndex }

                val prevThreshold = if (prevItemInfo != null) {
                    prevItemInfo.offset + prevItemInfo.size / 2f
                } else {
                    val currInfo = (if (draggedItemId != null) visibleItems.firstOrNull { it.key == draggedItemId } else null)
                        ?: visibleItems.firstOrNull { it.index == activeIdx + indexOffset }
                    if (currInfo != null) {
                        currInfo.offset - itemHeight / 2f
                    } else {
                        initialItemOffset + (activeIdx - originalIndex) * itemHeight
                    }
                }

                if (prevThreshold != null && center < prevThreshold) {
                    val toIndex = activeIdx - 1
                    currentIndex = toIndex
                    targetIndex = toIndex
                    onMove(activeIdx, toIndex)
                    hasMoved = true
                    continue
                }
            }
        }
    }

    /** Static target index calculation for floating preview mode (non-live reordering) */
    private fun updateTargetIndexStatic(listSize: Int) {
        val currentDragged = currentIndex
        if (currentDragged !in 0 until listSize) return
        val floatingCenter = floatingCenterY

        val visibleItems = lazyListState.layoutInfo.visibleItemsInfo
            .filter { it.index in indexOffset until (indexOffset + listSize) }

        if (visibleItems.isNotEmpty()) {
            val first = visibleItems.first()
            val last = visibleItems.last()
            val computedTarget = when {
                floatingCenter <= first.offset + first.size / 2f -> {
                    first.index - indexOffset
                }
                floatingCenter >= last.offset + last.size / 2f -> {
                    last.index - indexOffset
                }
                else -> {
                    val directHit = visibleItems.firstOrNull { item ->
                        floatingCenter >= item.offset && floatingCenter <= (item.offset + item.size)
                    }
                    if (directHit != null) {
                        directHit.index - indexOffset
                    } else {
                        visibleItems.minByOrNull { item ->
                            val c = item.offset + item.size / 2f
                            abs(c - floatingCenter)
                        }?.let { it.index - indexOffset } ?: (targetIndex.takeIf { it != -1 } ?: currentDragged)
                    }
                }
            }
            targetIndex = computedTarget.coerceIn(0, listSize - 1)
        }
    }

    fun onDragEnd() {
        if (!isDragging) return
        val from = originalIndex
        val to = if (liveReorder) currentIndex else targetIndex
        resetDragState()

        if (!liveReorder && from != -1 && to != -1 && from != to) {
            onMove(from, to)
        }
        onDragEnd.invoke()
    }

    fun onDragCancel() {
        if (!isDragging) return
        val original = originalIndex
        val current = currentIndex
        resetDragState()

        if (liveReorder && original != -1 && current != -1 && original != current) {
            onMove(current, original)
        }
        onDragCancel.invoke()
    }

    fun resetDragState() {
        autoScrollJob?.cancel()
        autoScrollJob = null
        isDragging = false
        draggedItemId = null
        originalIndex = -1
        currentIndex = -1
        targetIndex = -1
        dragOffsetY = 0f
    }

    private fun startAutoScrollLoop() {
        autoScrollJob?.cancel()
        autoScrollJob = scope.launch {
            while (isActive && isDragging) {
                val layoutInfo = lazyListState.layoutInfo
                val viewportStart = layoutInfo.viewportStartOffset.toFloat()
                val viewportEnd = layoutInfo.viewportEndOffset.toFloat()
                val edgeThreshold = 120f

                val top = clampedFloatingItemTop
                val bottom = top + itemHeight

                val canScrollUp = lazyListState.canScrollBackward
                val canScrollDown = lazyListState.canScrollForward

                val scrollDelta = when {
                    top < viewportStart + edgeThreshold && canScrollUp -> {
                        val distance = (viewportStart + edgeThreshold - top).coerceIn(0f, edgeThreshold)
                        val ratio = (distance / edgeThreshold).coerceIn(0.2f, 1f)
                        -14f * ratio
                    }
                    bottom > viewportEnd - edgeThreshold && canScrollDown -> {
                        val distance = (bottom - (viewportEnd - edgeThreshold)).coerceIn(0f, edgeThreshold)
                        val ratio = (distance / edgeThreshold).coerceIn(0.2f, 1f)
                        14f * ratio
                    }
                    else -> 0f
                }

                if (scrollDelta != 0f) {
                    lazyListState.scrollBy(scrollDelta)
                    if (currentListSize > 0) {
                        if (liveReorder) {
                            checkAndPerformReorder(currentListSize)
                        } else {
                            updateTargetIndexStatic(currentListSize)
                        }
                    }
                }

                delay(16)
            }
        }
    }
}

@Composable
fun rememberDragDropListState(
    lazyListState: LazyListState,
    onMove: (Int, Int) -> Unit,
    onDragEnd: () -> Unit = {},
    onDragCancel: () -> Unit = {},
    indexOffset: Int = 0,
    keyProvider: ((Int) -> Any)? = null,
    liveReorder: Boolean = true
): DragDropListState {
    val scope = rememberCoroutineScope()
    val state = remember(lazyListState, indexOffset, liveReorder) {
        DragDropListState(
            lazyListState = lazyListState,
            onMove = onMove,
            onDragEnd = onDragEnd,
            onDragCancel = onDragCancel,
            scope = scope,
            indexOffset = indexOffset,
            keyProvider = keyProvider,
            liveReorder = liveReorder
        )
    }

    // Keep dynamic callbacks up to date
    state.onMove = onMove
    state.onDragEnd = onDragEnd
    state.onDragCancel = onDragCancel
    state.keyProvider = keyProvider
    state.indexOffset = indexOffset

    DisposableEffect(state) {
        onDispose {
            state.resetDragState()
        }
    }

    return state
}

/**
 * Standard Six-Dot Drag Handle (⠿) component.
 */
@Composable
fun SixDotDragHandle(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
) {
    Canvas(modifier = modifier.size(width = 16.dp, height = 22.dp)) {
        val dotRadius = 2.2.dp.toPx()
        val spacingX = 6.dp.toPx()
        val spacingY = 6.dp.toPx()
        val startX = (size.width - spacingX) / 2f
        val startY = (size.height - spacingY * 2) / 2f

        for (col in 0..1) {
            for (row in 0..2) {
                drawCircle(
                    color = tint,
                    radius = dotRadius,
                    center = Offset(startX + col * spacingX, startY + row * spacingY)
                )
            }
        }
    }
}

/**
 * Modifier applied to a drag handle to initiate reordering via long press.
 */
@Composable
fun Modifier.reorderableDragHandle(
    key: Any,
    index: Int,
    listSize: Int,
    state: DragDropListState,
    onLongPressFeedback: () -> Unit = {}
): Modifier {
    val currentKey by rememberUpdatedState(key)
    val currentIndex by rememberUpdatedState(index)
    val currentListSize by rememberUpdatedState(listSize)
    val currentFeedback by rememberUpdatedState(onLongPressFeedback)

    return this.pointerInput(currentKey) {
        detectDragGesturesAfterLongPress(
            onDragStart = {
                currentFeedback()
                state.onDragStart(currentKey, currentIndex, currentListSize)
            },
            onDrag = { change, dragAmount ->
                change.consume()
                state.onDrag(dragAmount.y, currentListSize)
            },
            onDragEnd = {
                state.onDragEnd()
            },
            onDragCancel = {
                state.onDragCancel()
            }
        )
    }
}

/**
 * Modifier applied to an entire list item row to support long-press drag-and-drop
 * while allowing normal short taps to trigger [onClick].
 */
@Composable
fun Modifier.reorderableDragRow(
    key: Any,
    index: Int,
    listSize: Int,
    state: DragDropListState,
    enabled: Boolean = true,
    onLongPressFeedback: () -> Unit = {},
    onClick: () -> Unit
): Modifier {
    val currentKey by rememberUpdatedState(key)
    val currentIndex by rememberUpdatedState(index)
    val currentListSize by rememberUpdatedState(listSize)
    val currentOnClick by rememberUpdatedState(onClick)
    val currentFeedback by rememberUpdatedState(onLongPressFeedback)

    return this
        .pointerInput(currentKey, enabled) {
            if (!enabled) return@pointerInput
            detectDragGesturesAfterLongPress(
                onDragStart = {
                    currentFeedback()
                    state.onDragStart(currentKey, currentIndex, currentListSize)
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    state.onDrag(dragAmount.y, currentListSize)
                },
                onDragEnd = {
                    state.onDragEnd()
                },
                onDragCancel = {
                    state.onDragCancel()
                }
            )
        }
        .clickable(enabled = enabled) {
            if (!state.isDragging) {
                currentOnClick()
            }
        }
}

/**
 * Modifier applied to an entire list item to enable long-press drag-and-drop reordering.
 */
@Composable
fun Modifier.dragDropItem(
    index: Int,
    listSize: Int,
    state: DragDropListState
): Modifier {
    val currentIndex by rememberUpdatedState(index)
    val currentListSize by rememberUpdatedState(listSize)

    return this
        .graphicsLayer {
            alpha = if (state.draggedIndex == currentIndex) 0.35f else 1f
        }
        .pointerInput(Unit) {
            detectDragGesturesAfterLongPress(
                onDragStart = {
                    state.onDragStart(currentIndex, currentListSize)
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    state.onDrag(dragAmount.y, currentListSize)
                },
                onDragEnd = {
                    state.onDragEnd()
                },
                onDragCancel = {
                    state.onDragCancel()
                }
            )
        }
}

/**
 * Modifier applied to the category drag handle icon for immediate vertical drag-and-drop on touch.
 */
@Composable
fun Modifier.categoryDragHandle(
    index: Int,
    listSize: Int,
    state: DragDropListState
): Modifier {
    val currentIndex by rememberUpdatedState(index)
    val currentListSize by rememberUpdatedState(listSize)

    return this.pointerInput(Unit) {
        detectVerticalDragGestures(
            onDragStart = {
                state.onDragStart(currentIndex, currentListSize)
            },
            onVerticalDrag = { change, dragAmount ->
                change.consume()
                state.onDrag(dragAmount, currentListSize)
            },
            onDragEnd = {
                state.onDragEnd()
            },
            onDragCancel = {
                state.onDragCancel()
            }
        )
    }
}

/** Backward compatible alias for dragHandle */
@Composable
fun Modifier.dragHandle(
    index: Int,
    listSize: Int,
    state: DragDropListState
): Modifier = categoryDragHandle(index = index, listSize = listSize, state = state)
