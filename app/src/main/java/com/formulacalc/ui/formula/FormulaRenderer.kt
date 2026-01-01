package com.formulacalc.ui.formula

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.formulacalc.model.*

/**
 * Цвета для элементов формулы
 */
object FormulaColors {
    val variableGradientStart = Color(0xFF3B82F6)
    val variableGradientEnd = Color(0xFF2563EB)
    val variableShadow = Color(0x663B82F6)

    val operatorColor = Color(0xFF64748B)

    val ellipsisGradientStart = Color(0xFFFEF3C7)
    val ellipsisGradientEnd = Color(0xFFFDE68A)
    val ellipsisText = Color(0xFFD97706)

    val fractionLine = Color(0xFFA855F7)
    val fractionBackground = Color(0x0D9333EA)

    val dropIndicatorGreen = Color(0xFF22C55E)
    val dropIndicatorPurple = Color(0xFFA855F7)
}

/**
 * Информация о границах элемента для hit testing
 */
data class ElementBounds(
    val id: String,
    val bounds: Rect,
    val canBeDropTarget: Boolean = true
)

/**
 * Состояние drag для элемента
 */
data class DragState(
    val isDragging: Boolean = false,
    val draggedElement: FormulaElement? = null,
    val currentPosition: Offset = Offset.Zero,
    val startPosition: Offset = Offset.Zero
)

/**
 * Состояние hover для drop indicator
 */
data class HoverState(
    val targetId: String? = null,
    val side: DropSide? = null
)

/**
 * Глобальное хранилище границ элементов
 */
class ElementBoundsRegistry {
    private val bounds = mutableMapOf<String, ElementBounds>()

    fun register(id: String, rect: Rect, canBeDropTarget: Boolean = true) {
        bounds[id] = ElementBounds(id, rect, canBeDropTarget)
    }

    fun unregister(id: String) {
        bounds.remove(id)
    }

    fun clear() {
        bounds.clear()
    }

    /**
     * Найти элемент и сторону drop по позиции курсора
     */
    fun findDropTarget(position: Offset, excludeId: String?): Pair<String, DropSide>? {
        for ((id, elementBounds) in bounds) {
            if (id == excludeId || !elementBounds.canBeDropTarget) continue

            val rect = elementBounds.bounds
            if (rect.contains(position)) {
                // Определяем сторону
                val relX = (position.x - rect.left) / rect.width
                val relY = (position.y - rect.top) / rect.height

                // Горизонтальные края (20% от краёв)
                val isLeftEdge = relX < 0.2f
                val isRightEdge = relX > 0.8f
                // Вертикальные края (25% от краёв)
                val isTopEdge = relY < 0.25f
                val isBottomEdge = relY > 0.75f

                val side = when {
                    isLeftEdge -> DropSide.LEFT
                    isRightEdge -> DropSide.RIGHT
                    isTopEdge -> DropSide.TOP
                    isBottomEdge -> DropSide.BOTTOM
                    // По умолчанию - справа
                    else -> DropSide.RIGHT
                }

                return id to side
            }
        }
        return null
    }
}

val LocalElementBoundsRegistry = compositionLocalOf { ElementBoundsRegistry() }

/**
 * Рендеринг формулы с drag & drop
 */
@Composable
fun FormulaRenderer(
    elements: List<FormulaElement>,
    modifier: Modifier = Modifier,
    dragState: DragState = DragState(),
    hoverState: HoverState = HoverState(),
    onDragStart: (FormulaElement, Offset) -> Unit = { _, _ -> },
    onDragEnd: () -> Unit = {},
    onDrag: (Offset) -> Unit = {},
    onHover: (String?, DropSide?) -> Unit = { _, _ -> },
    onEllipsisClick: (String) -> Unit = {},
    onVariableClick: (String) -> Unit = {},
    nestingLevel: Int = 0
) {
    val scale = when (nestingLevel) {
        0 -> 1f
        1 -> 0.85f
        else -> 0.80f
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        elements.forEach { element ->
            FormulaElementView(
                element = element,
                scale = scale,
                dragState = dragState,
                hoverState = hoverState,
                onDragStart = onDragStart,
                onDragEnd = onDragEnd,
                onDrag = onDrag,
                onHover = onHover,
                onEllipsisClick = onEllipsisClick,
                onVariableClick = onVariableClick,
                nestingLevel = nestingLevel
            )
        }
    }
}

/**
 * Отрисовка одного элемента формулы
 */
@Composable
private fun FormulaElementView(
    element: FormulaElement,
    scale: Float,
    dragState: DragState,
    hoverState: HoverState,
    onDragStart: (FormulaElement, Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDrag: (Offset) -> Unit,
    onHover: (String?, DropSide?) -> Unit,
    onEllipsisClick: (String) -> Unit,
    onVariableClick: (String) -> Unit,
    nestingLevel: Int
) {
    val isDraggedElement = dragState.draggedElement?.id == element.id
    val isHovered = hoverState.targetId == element.id

    // Анимация пульсации для индикатора
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = Modifier.padding(horizontal = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        // Drop indicators
        if (isHovered && dragState.isDragging) {
            when (hoverState.side) {
                DropSide.LEFT -> DropIndicatorVertical(
                    modifier = Modifier.align(Alignment.CenterStart),
                    alpha = pulseAlpha
                )
                DropSide.RIGHT -> DropIndicatorVertical(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    alpha = pulseAlpha
                )
                DropSide.TOP -> DropIndicatorHorizontal(
                    modifier = Modifier.align(Alignment.TopCenter),
                    alpha = pulseAlpha
                )
                DropSide.BOTTOM -> DropIndicatorHorizontal(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    alpha = pulseAlpha
                )
                null -> {}
            }
        }

        when (element) {
            is FormulaElement.Variable -> DraggableVariableView(
                variable = element,
                scale = scale,
                isDragged = isDraggedElement,
                onClick = { onVariableClick(element.id) },
                onDragStart = { offset -> onDragStart(element, offset) },
                onDragEnd = onDragEnd,
                onDrag = onDrag
            )

            is FormulaElement.Operator -> OperatorView(
                operator = element,
                scale = scale,
                isDragged = isDraggedElement
            )

            is FormulaElement.Equals -> EqualsView(scale = scale)

            is FormulaElement.Ellipsis -> EllipsisView(
                scale = scale,
                onClick = { onEllipsisClick(element.id) }
            )

            is FormulaElement.Fraction -> FractionView(
                fraction = element,
                scale = scale,
                isDragged = isDraggedElement,
                dragState = dragState,
                hoverState = hoverState,
                onDragStart = onDragStart,
                onDragEnd = onDragEnd,
                onDrag = onDrag,
                onHover = onHover,
                onEllipsisClick = onEllipsisClick,
                onVariableClick = onVariableClick,
                nestingLevel = nestingLevel
            )
        }
    }
}

/**
 * Переменная с поддержкой drag
 */
@Composable
private fun DraggableVariableView(
    variable: FormulaElement.Variable,
    scale: Float,
    isDragged: Boolean,
    onClick: () -> Unit,
    onDragStart: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDrag: (Offset) -> Unit
) {
    val boundsRegistry = LocalElementBoundsRegistry.current
    val fontSize = (28 * scale).sp
    val exponentSize = (16 * scale).sp
    val paddingH = (14 * scale).dp
    val paddingV = (8 * scale).dp

    Row(
        modifier = Modifier
            .onGloballyPositioned { coordinates ->
                boundsRegistry.register(
                    variable.id,
                    coordinates.boundsInRoot(),
                    canBeDropTarget = true
                )
            }
            .graphicsLayer {
                alpha = if (isDragged) 0.4f else 1f
                scaleX = if (isDragged) 0.95f else 1f
                scaleY = if (isDragged) 0.95f else 1f
            }
            .clip(RoundedCornerShape(12.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        FormulaColors.variableGradientStart,
                        FormulaColors.variableGradientEnd
                    )
                )
            )
            .clickable { onClick() }
            .pointerInput(variable.id) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        onDragStart(offset)
                    },
                    onDragEnd = {
                        onDragEnd()
                    },
                    onDragCancel = {
                        onDragEnd()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount)
                    }
                )
            }
            .padding(horizontal = paddingH, vertical = paddingV),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = variable.displayValue,
            color = Color.White,
            fontSize = fontSize,
            fontWeight = FontWeight.Medium,
            fontStyle = FontStyle.Italic
        )

        // Экспонента
        variable.exponent?.let { exp ->
            Text(
                text = when (exp) {
                    is Exponent.Simple -> exp.value
                    is Exponent.Fraction -> "${exp.numerator}/${exp.denominator}"
                },
                color = Color.White.copy(alpha = 0.9f),
                fontSize = exponentSize,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.offset(y = (-4).dp)
            )
        }
    }

    DisposableEffect(variable.id) {
        onDispose {
            boundsRegistry.unregister(variable.id)
        }
    }
}

/**
 * Оператор
 */
@Composable
private fun OperatorView(
    operator: FormulaElement.Operator,
    scale: Float,
    isDragged: Boolean
) {
    Text(
        text = operator.symbol,
        color = FormulaColors.operatorColor,
        fontSize = (28 * scale).sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .alpha(if (isDragged) 0.4f else 1f)
    )
}

/**
 * Знак равенства
 */
@Composable
private fun EqualsView(scale: Float) {
    Text(
        text = "=",
        color = FormulaColors.operatorColor,
        fontSize = (28 * scale).sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(horizontal = 8.dp)
    )
}

/**
 * Ellipsis (···)
 */
@Composable
private fun EllipsisView(
    scale: Float,
    onClick: () -> Unit
) {
    Text(
        text = "···",
        color = FormulaColors.ellipsisText,
        fontSize = (24 * scale).sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        FormulaColors.ellipsisGradientStart,
                        FormulaColors.ellipsisGradientEnd
                    )
                )
            )
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

/**
 * Дробь
 */
@Composable
private fun FractionView(
    fraction: FormulaElement.Fraction,
    scale: Float,
    isDragged: Boolean,
    dragState: DragState,
    hoverState: HoverState,
    onDragStart: (FormulaElement, Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDrag: (Offset) -> Unit,
    onHover: (String?, DropSide?) -> Unit,
    onEllipsisClick: (String) -> Unit,
    onVariableClick: (String) -> Unit,
    nestingLevel: Int
) {
    val boundsRegistry = LocalElementBoundsRegistry.current

    Column(
        modifier = Modifier
            .onGloballyPositioned { coordinates ->
                boundsRegistry.register(
                    fraction.id,
                    coordinates.boundsInRoot(),
                    canBeDropTarget = true
                )
            }
            .graphicsLayer {
                alpha = if (isDragged) 0.4f else 1f
            }
            .clip(RoundedCornerShape(12.dp))
            .background(FormulaColors.fractionBackground)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Числитель
        FormulaRenderer(
            elements = fraction.numerator,
            dragState = dragState,
            hoverState = hoverState,
            onDragStart = onDragStart,
            onDragEnd = onDragEnd,
            onDrag = onDrag,
            onHover = onHover,
            onEllipsisClick = onEllipsisClick,
            onVariableClick = onVariableClick,
            nestingLevel = nestingLevel + 1
        )

        // Линия дроби
        Box(
            modifier = Modifier
                .padding(vertical = 4.dp)
                .height(3.dp)
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            FormulaColors.fractionLine,
                            FormulaColors.fractionLine.copy(alpha = 0.8f)
                        )
                    ),
                    shape = RoundedCornerShape(2.dp)
                )
        )

        // Знаменатель
        FormulaRenderer(
            elements = fraction.denominator,
            dragState = dragState,
            hoverState = hoverState,
            onDragStart = onDragStart,
            onDragEnd = onDragEnd,
            onDrag = onDrag,
            onHover = onHover,
            onEllipsisClick = onEllipsisClick,
            onVariableClick = onVariableClick,
            nestingLevel = nestingLevel + 1
        )
    }

    DisposableEffect(fraction.id) {
        onDispose {
            boundsRegistry.unregister(fraction.id)
        }
    }
}

/**
 * Вертикальный индикатор drop (зелёный - для вставки слева/справа)
 */
@Composable
private fun DropIndicatorVertical(
    modifier: Modifier = Modifier,
    alpha: Float = 1f
) {
    Box(
        modifier = modifier
            .width(4.dp)
            .height(40.dp)
            .graphicsLayer { this.alpha = alpha }
            .background(
                FormulaColors.dropIndicatorGreen,
                RoundedCornerShape(2.dp)
            )
    )
}

/**
 * Горизонтальный индикатор drop (фиолетовый - для создания дроби)
 */
@Composable
private fun DropIndicatorHorizontal(
    modifier: Modifier = Modifier,
    alpha: Float = 1f
) {
    Box(
        modifier = modifier
            .height(4.dp)
            .width(60.dp)
            .graphicsLayer { this.alpha = alpha }
            .background(
                FormulaColors.dropIndicatorPurple,
                RoundedCornerShape(2.dp)
            )
    )
}
