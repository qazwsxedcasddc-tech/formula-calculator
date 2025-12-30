package com.formulacalc.ui

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.formulacalc.model.FormulaToken
import com.formulacalc.model.PresetFormula
import kotlin.math.roundToInt

/**
 * Данные перетаскивания
 */
sealed class DragData {
    data class Token(val token: FormulaToken) : DragData()
    data class Preset(val preset: PresetFormula) : DragData()
}

/**
 * Состояние drag & drop
 */
internal class DragDropState {
    var isDragging by mutableStateOf(false)
    var dragPosition by mutableStateOf(Offset.Zero)
    var dragData by mutableStateOf<DragData?>(null)
    var dragOffset by mutableStateOf(Offset.Zero)

    // Зона drop target
    var dropTargetPosition by mutableStateOf(Offset.Zero)
    var dropTargetSize by mutableStateOf(IntSize.Zero)

    fun startDrag(data: DragData, startPosition: Offset) {
        isDragging = true
        dragData = data
        dragPosition = startPosition
        dragOffset = Offset.Zero
    }

    fun updateDrag(offset: Offset) {
        dragOffset = offset
    }

    fun endDrag(): DragData? {
        val data = dragData
        isDragging = false
        dragData = null
        dragOffset = Offset.Zero
        return data
    }

    fun isOverDropTarget(): Boolean {
        if (!isDragging) return false
        val currentPos = dragPosition + dragOffset
        return currentPos.x >= dropTargetPosition.x &&
                currentPos.x <= dropTargetPosition.x + dropTargetSize.width &&
                currentPos.y >= dropTargetPosition.y &&
                currentPos.y <= dropTargetPosition.y + dropTargetSize.height
    }
}

/**
 * CompositionLocal для состояния drag & drop
 */
val LocalDragDropState = compositionLocalOf { DragDropState() }

/**
 * Провайдер drag & drop контекста
 */
@Composable
fun DragDropProvider(
    content: @Composable () -> Unit
) {
    val state = remember { DragDropState() }
    CompositionLocalProvider(LocalDragDropState provides state) {
        Box {
            content()

            // Плавающий элемент при перетаскивании
            if (state.isDragging && state.dragData != null) {
                DragOverlay(
                    data = state.dragData!!,
                    offset = state.dragPosition + state.dragOffset
                )
            }
        }
    }
}

/**
 * Плавающий элемент при перетаскивании
 */
@Composable
private fun DragOverlay(
    data: DragData,
    offset: Offset
) {
    Box(
        modifier = Modifier
            .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
            .graphicsLayer {
                alpha = 0.8f
                scaleX = 1.1f
                scaleY = 1.1f
            }
    ) {
        when (data) {
            is DragData.Token -> {
                DragTokenPreview(token = data.token)
            }
            is DragData.Preset -> {
                DragPresetPreview(preset = data.preset)
            }
        }
    }
}

/**
 * Превью токена при перетаскивании
 */
@Composable
private fun DragTokenPreview(token: FormulaToken) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        shadowElevation = 8.dp
    ) {
        Text(
            text = token.displayText,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(12.dp)
        )
    }
}

/**
 * Превью формулы при перетаскивании
 */
@Composable
private fun DragPresetPreview(preset: PresetFormula) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        shadowElevation = 8.dp
    ) {
        Text(
            text = preset.toDisplayString(),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(12.dp)
        )
    }
}

/**
 * Модификатор для draggable элемента
 */
@Composable
fun Modifier.draggableToken(
    token: FormulaToken,
    onClick: () -> Unit = {}
): Modifier {
    val state = LocalDragDropState.current
    var itemPosition by remember { mutableStateOf(Offset.Zero) }

    return this
        .onGloballyPositioned { coordinates ->
            itemPosition = coordinates.positionInRoot()
        }
        .pointerInput(token) {
            detectDragGesturesAfterLongPress(
                onDragStart = { offset ->
                    state.startDrag(DragData.Token(token), itemPosition + offset)
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    state.updateDrag(state.dragOffset + dragAmount)
                },
                onDragEnd = {
                    state.endDrag()
                },
                onDragCancel = {
                    state.endDrag()
                }
            )
        }
}

/**
 * Модификатор для draggable формулы
 */
@Composable
fun Modifier.draggablePreset(
    preset: PresetFormula,
    onClick: () -> Unit = {}
): Modifier {
    val state = LocalDragDropState.current
    var itemPosition by remember { mutableStateOf(Offset.Zero) }

    return this
        .onGloballyPositioned { coordinates ->
            itemPosition = coordinates.positionInRoot()
        }
        .pointerInput(preset) {
            detectDragGesturesAfterLongPress(
                onDragStart = { offset ->
                    state.startDrag(DragData.Preset(preset), itemPosition + offset)
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    state.updateDrag(state.dragOffset + dragAmount)
                },
                onDragEnd = {
                    state.endDrag()
                },
                onDragCancel = {
                    state.endDrag()
                }
            )
        }
}

/**
 * Модификатор для drop target
 */
@Composable
fun Modifier.dropTarget(
    onDragOver: (Boolean) -> Unit,
    onDrop: (DragData) -> Unit
): Modifier {
    val state = LocalDragDropState.current

    LaunchedEffect(state.isDragging, state.dragOffset) {
        onDragOver(state.isOverDropTarget())
    }

    LaunchedEffect(state.isDragging) {
        if (!state.isDragging && state.dragData != null) {
            if (state.isOverDropTarget()) {
                state.dragData?.let { onDrop(it) }
            }
        }
    }

    return this.onGloballyPositioned { coordinates ->
        state.dropTargetPosition = coordinates.positionInRoot()
        state.dropTargetSize = coordinates.size
    }
}
