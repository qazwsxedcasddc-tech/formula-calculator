package com.formulacalc.ui.formula

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.util.Log
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

    // Скобки
    val parenthesesBorder = Color(0xFF10B981) // Изумрудный
    val parenthesesBackground = Color(0x0D10B981)
    val parenthesesSymbol = Color(0xFF10B981)
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
    val fingerPosition: Offset = Offset.Zero, // Абсолютная позиция пальца на экране
    val startPosition: Offset = Offset.Zero // Начальная позиция при начале drag
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
    private val bounds = mutableStateMapOf<String, ElementBounds>()

    // Границы области формулы (для определения "далеко за пределами")
    var formulaAreaBounds: Rect? = null
        private set

    fun register(id: String, rect: Rect, canBeDropTarget: Boolean = true) {
        bounds[id] = ElementBounds(id, rect, canBeDropTarget)
    }

    fun unregister(id: String) {
        bounds.remove(id)
    }

    fun registerFormulaArea(rect: Rect) {
        formulaAreaBounds = rect
    }

    fun clear() {
        bounds.clear()
        // formulaAreaBounds не очищаем, т.к. область всегда существует
    }

    /**
     * Проверить, находится ли позиция внутри области формулы (с небольшим отступом)
     */
    fun isInsideFormulaArea(position: Offset, margin: Float = 50f): Boolean {
        val area = formulaAreaBounds ?: return true // Если не задано, считаем что внутри
        return position.x >= area.left - margin &&
               position.x <= area.right + margin &&
               position.y >= area.top - margin &&
               position.y <= area.bottom + margin
    }

    /**
     * Логирование всех зарегистрированных элементов
     */
    fun logAllBounds(prefix: String = "") {
        Log.d("DragDrop", "$prefix Registered bounds (${bounds.size} elements):")
        bounds.forEach { (id, elementBounds) ->
            val rect = elementBounds.bounds
            Log.d("DragDrop", "  - $id: [${rect.left.toInt()},${rect.top.toInt()} - ${rect.right.toInt()},${rect.bottom.toInt()}] canDrop=${elementBounds.canBeDropTarget}")
        }
    }

    /**
     * Найти элемент и сторону drop по абсолютной позиции пальца
     */
    fun findDropTarget(fingerPosition: Offset, excludeId: String?): Pair<String, DropSide>? {
        for ((id, elementBounds) in bounds) {
            if (id == excludeId || !elementBounds.canBeDropTarget) continue

            val rect = elementBounds.bounds
            if (rect.contains(fingerPosition)) {
                // Вычисляем относительную позицию внутри элемента
                val relX = (fingerPosition.x - rect.left) / rect.width
                val relY = (fingerPosition.y - rect.top) / rect.height

                // Расстояние от центра (нормализованное)
                val distFromCenterX = kotlin.math.abs(relX - 0.5f) * 2 // 0..1
                val distFromCenterY = kotlin.math.abs(relY - 0.5f) * 2 // 0..1

                // Определяем направление как в веб-версии
                val side = when {
                    // Если ближе к верху/низу чем к бокам
                    distFromCenterY > distFromCenterX && relY < 0.5f -> DropSide.TOP
                    distFromCenterY > distFromCenterX && relY >= 0.5f -> DropSide.BOTTOM
                    relX < 0.5f -> DropSide.LEFT
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
 * Форматирование числа для отображения
 */
private fun formatNumber(value: Double): String {
    return if (value == value.toLong().toDouble()) {
        value.toLong().toString()
    } else {
        String.format("%.4g", value)
    }
}

/**
 * Рендеринг формулы с drag & drop и авто-масштабированием
 */
@Composable
fun FormulaRenderer(
    elements: List<FormulaElement>,
    modifier: Modifier = Modifier,
    dragState: DragState = DragState(),
    hoverState: HoverState = HoverState(),
    onDragStart: (FormulaElement, Offset) -> Unit = { _, _ -> },
    onDragEnd: () -> Unit = {},
    onDragMove: (Offset) -> Unit = {}, // Теперь передаём абсолютную позицию
    onEllipsisClick: (String) -> Unit = {},
    onVariableClick: (String) -> Unit = {},
    onParenthesesClick: (String) -> Unit = {}, // Клик на скобки
    onOperatorClick: (String) -> Unit = {}, // Клик на оператор для изменения
    nestingLevel: Int = 0,
    maxWidth: androidx.compose.ui.unit.Dp? = null,  // Для авто-масштабирования
    maxHeight: androidx.compose.ui.unit.Dp? = null,
    variableValues: Map<String, Double> = emptyMap() // Значения переменных
) {
    // Базовый масштаб для вложенности
    val nestingScale = when (nestingLevel) {
        0 -> 1f
        1 -> 0.85f
        else -> 0.80f
    }

    // Для корневого уровня (nestingLevel=0) вычисляем глобальный масштаб
    if (nestingLevel == 0 && maxWidth != null && maxHeight != null) {
        // Измеряем содержимое и масштабируем
        var contentWidth by remember { mutableStateOf(0f) }
        var contentHeight by remember { mutableStateOf(0f) }

        val density = androidx.compose.ui.platform.LocalDensity.current
        val maxWidthPx = with(density) { maxWidth.toPx() }
        val maxHeightPx = with(density) { maxHeight.toPx() }

        // Вычисляем масштаб
        val globalScale = remember(contentWidth, contentHeight, maxWidthPx, maxHeightPx) {
            if (contentWidth > 0 && contentHeight > 0) {
                val scaleX = if (contentWidth > maxWidthPx) maxWidthPx / contentWidth else 1f
                val scaleY = if (contentHeight > maxHeightPx) maxHeightPx / contentHeight else 1f
                minOf(scaleX, scaleY, 1f).coerceIn(0.4f, 1f)
            } else {
                1f
            }
        }

        Box(
            modifier = modifier
                .graphicsLayer {
                    scaleX = globalScale
                    scaleY = globalScale
                    // Центрируем после масштабирования
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin.Center
                },
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.onGloballyPositioned { coordinates ->
                    // Измеряем оригинальный размер (до масштабирования)
                    contentWidth = coordinates.size.width.toFloat()
                    contentHeight = coordinates.size.height.toFloat()
                },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                elements.forEach { element ->
                    FormulaElementView(
                        element = element,
                        scale = nestingScale,
                        dragState = dragState,
                        hoverState = hoverState,
                        onDragStart = onDragStart,
                        onDragEnd = onDragEnd,
                        onDragMove = onDragMove,
                        onEllipsisClick = onEllipsisClick,
                        onVariableClick = onVariableClick,
                        onParenthesesClick = onParenthesesClick,
                        onOperatorClick = onOperatorClick,
                        nestingLevel = nestingLevel,
                        variableValues = variableValues
                    )
                }
            }
        }
    } else {
        // Вложенный уровень — без авто-масштабирования
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            elements.forEach { element ->
                FormulaElementView(
                    element = element,
                    scale = nestingScale,
                    dragState = dragState,
                    hoverState = hoverState,
                    onDragStart = onDragStart,
                    onDragEnd = onDragEnd,
                    onDragMove = onDragMove,
                    onEllipsisClick = onEllipsisClick,
                    onVariableClick = onVariableClick,
                    onParenthesesClick = onParenthesesClick,
                    onOperatorClick = onOperatorClick,
                    nestingLevel = nestingLevel,
                    variableValues = variableValues
                )
            }
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
    onDragMove: (Offset) -> Unit,
    onEllipsisClick: (String) -> Unit,
    onVariableClick: (String) -> Unit,
    onParenthesesClick: (String) -> Unit,
    onOperatorClick: (String) -> Unit,
    variableValues: Map<String, Double> = emptyMap(),
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
        // Drop indicators — показываем только когда идёт drag И hover на этом элементе
        if (isHovered && dragState.isDragging && !isDraggedElement) {
            when (hoverState.side) {
                DropSide.LEFT -> DropIndicatorVertical(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = (-6).dp),
                    alpha = pulseAlpha
                )
                DropSide.RIGHT -> DropIndicatorVertical(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .offset(x = 6.dp),
                    alpha = pulseAlpha
                )
                DropSide.TOP -> DropIndicatorHorizontal(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-6).dp),
                    alpha = pulseAlpha
                )
                DropSide.BOTTOM -> DropIndicatorHorizontal(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = 6.dp),
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
                variableValue = variableValues[element.id], // Используем ID вместо имени
                onClick = { onVariableClick(element.id) },
                onDragStart = { offset -> onDragStart(element, offset) },
                onDragEnd = onDragEnd,
                onDragMove = onDragMove
            )

            is FormulaElement.Operator -> OperatorView(
                operator = element,
                scale = scale,
                onClick = { onOperatorClick(element.id) }
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
                onDragMove = onDragMove,
                onEllipsisClick = onEllipsisClick,
                onVariableClick = onVariableClick,
                onParenthesesClick = onParenthesesClick,
                onOperatorClick = onOperatorClick,
                nestingLevel = nestingLevel,
                variableValues = variableValues
            )

            is FormulaElement.Parentheses -> ParenthesesView(
                parentheses = element,
                scale = scale,
                isDragged = isDraggedElement,
                dragState = dragState,
                hoverState = hoverState,
                onDragStart = onDragStart,
                onDragEnd = onDragEnd,
                onDragMove = onDragMove,
                onEllipsisClick = onEllipsisClick,
                onVariableClick = onVariableClick,
                onParenthesesClick = onParenthesesClick,
                onOperatorClick = onOperatorClick,
                onClick = { onParenthesesClick(element.id) },
                nestingLevel = nestingLevel,
                variableValues = variableValues
            )
        }
    }
}

/**
 * Переменная с поддержкой drag (long press + move)
 * Показывает значение под переменной если оно задано
 */
@Composable
private fun DraggableVariableView(
    variable: FormulaElement.Variable,
    scale: Float,
    isDragged: Boolean,
    variableValue: Double? = null, // Значение переменной
    onClick: () -> Unit,
    onDragStart: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragMove: (Offset) -> Unit
) {
    val boundsRegistry = LocalElementBoundsRegistry.current
    val fontSize = (28 * scale).sp
    val exponentSize = (16 * scale).sp
    val valueSize = (12 * scale).sp
    val paddingH = (14 * scale).dp
    val paddingV = (8 * scale).dp

    // Позиция элемента на экране
    var elementPosition by remember { mutableStateOf(Offset.Zero) }

    // Цвет фона зависит от наличия значения
    val hasValue = variableValue != null
    val gradientColors = if (hasValue) {
        listOf(Color(0xFF22C55E), Color(0xFF16A34A)) // Зелёный если значение задано
    } else {
        listOf(FormulaColors.variableGradientStart, FormulaColors.variableGradientEnd)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .onGloballyPositioned { coordinates ->
                    val bounds = coordinates.boundsInRoot()
                    boundsRegistry.register(variable.id, bounds, canBeDropTarget = true)
                    elementPosition = Offset(bounds.left, bounds.top)
                }
                .graphicsLayer {
                    alpha = if (isDragged) 0.4f else 1f
                    scaleX = if (isDragged) 0.95f else 1f
                    scaleY = if (isDragged) 0.95f else 1f
                }
                .clip(RoundedCornerShape(12.dp))
                .background(brush = Brush.linearGradient(colors = gradientColors))
                .pointerInput(variable.id) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val longPress = awaitLongPressOrCancellation(down.id)

                        if (longPress != null) {
                            // Началось long press — начинаем drag
                            val absolutePosition = elementPosition + longPress.position
                            onDragStart(absolutePosition)

                            // Отслеживаем движение пальца
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: break

                                if (change.changedToUp()) {
                                    onDragEnd()
                                    break
                                }

                                val newAbsolutePosition = elementPosition + change.position
                                onDragMove(newAbsolutePosition)
                                change.consume()
                            }
                        } else {
                            // Это был короткий tap — onClick (ввод значения)
                            onClick()
                        }
                    }
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

        // Показываем значение под переменной
        if (variableValue != null) {
            Text(
                text = formatNumber(variableValue),
                color = Color(0xFF22C55E),
                fontSize = valueSize,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 2.dp)
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
 * Оператор — кликабельный для изменения
 */
@Composable
private fun OperatorView(
    operator: FormulaElement.Operator,
    scale: Float,
    onClick: () -> Unit = {}
) {
    Text(
        text = operator.symbol,
        color = FormulaColors.operatorColor,
        fontSize = (28 * scale).sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
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
    onDragMove: (Offset) -> Unit,
    onEllipsisClick: (String) -> Unit,
    onVariableClick: (String) -> Unit,
    onParenthesesClick: (String) -> Unit,
    onOperatorClick: (String) -> Unit,
    nestingLevel: Int,
    variableValues: Map<String, Double> = emptyMap()
) {
    val boundsRegistry = LocalElementBoundsRegistry.current

    // IntrinsicSize.Max заставляет Column измерить максимальную ширину детей
    // и использовать её для fillMaxWidth() у линии дроби
    Column(
        modifier = Modifier
            .width(IntrinsicSize.Max)
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
            onDragMove = onDragMove,
            onEllipsisClick = onEllipsisClick,
            onVariableClick = onVariableClick,
            onParenthesesClick = onParenthesesClick,
            onOperatorClick = onOperatorClick,
            nestingLevel = nestingLevel + 1,
            variableValues = variableValues
        )

        // Линия дроби — теперь растягивается по ширине самого широкого элемента
        Box(
            modifier = Modifier
                .padding(vertical = 4.dp)
                .height(3.dp)
                .widthIn(min = 40.dp)
                .fillMaxWidth()
                .background(
                    color = FormulaColors.fractionLine,
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
            onDragMove = onDragMove,
            onEllipsisClick = onEllipsisClick,
            onVariableClick = onVariableClick,
            onParenthesesClick = onParenthesesClick,
            onOperatorClick = onOperatorClick,
            nestingLevel = nestingLevel + 1,
            variableValues = variableValues
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

/**
 * Скобки — контейнер для группировки элементов
 * Отображает ( содержимое ) с возможностью перетаскивания всей группы
 * Короткий тап открывает диалог для разворачивания скобок
 */
@Composable
private fun ParenthesesView(
    parentheses: FormulaElement.Parentheses,
    scale: Float,
    isDragged: Boolean,
    dragState: DragState,
    hoverState: HoverState,
    onDragStart: (FormulaElement, Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragMove: (Offset) -> Unit,
    onEllipsisClick: (String) -> Unit,
    onVariableClick: (String) -> Unit,
    onParenthesesClick: (String) -> Unit,
    onOperatorClick: (String) -> Unit,
    onClick: () -> Unit, // Короткий тап на скобки
    nestingLevel: Int,
    variableValues: Map<String, Double> = emptyMap()
) {
    val boundsRegistry = LocalElementBoundsRegistry.current
    val parenFontSize = (32 * scale).sp

    // Позиция элемента на экране для drag
    var elementPosition by remember { mutableStateOf(Offset.Zero) }

    Row(
        modifier = Modifier
            .onGloballyPositioned { coordinates ->
                val bounds = coordinates.boundsInRoot()
                boundsRegistry.register(parentheses.id, bounds, canBeDropTarget = true)
                elementPosition = Offset(bounds.left, bounds.top)
            }
            .graphicsLayer {
                alpha = if (isDragged) 0.4f else 1f
                scaleX = if (isDragged) 0.95f else 1f
                scaleY = if (isDragged) 0.95f else 1f
            }
            .clip(RoundedCornerShape(8.dp))
            .background(FormulaColors.parenthesesBackground)
            .pointerInput(parentheses.id) {
                detectTapGestures(
                    onTap = {
                        // Короткий тап — открываем диалог для действий со скобками
                        onClick()
                    },
                    onLongPress = { offset ->
                        // Long press — начинаем drag
                        val absolutePosition = elementPosition + offset
                        onDragStart(parentheses, absolutePosition)
                    }
                )
            }
            .pointerInput(parentheses.id) {
                // Отслеживаем движение после начала drag
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val longPress = awaitLongPressOrCancellation(down.id)

                    if (longPress != null) {
                        // Отслеживаем движение пальца
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break

                            if (change.changedToUp()) {
                                onDragEnd()
                                break
                            }

                            val newAbsolutePosition = elementPosition + change.position
                            onDragMove(newAbsolutePosition)
                            change.consume()
                        }
                    }
                }
            }
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Открывающая скобка
        Text(
            text = "(",
            color = FormulaColors.parenthesesSymbol,
            fontSize = parenFontSize,
            fontWeight = FontWeight.Medium
        )

        // Содержимое скобок
        if (parentheses.children.isEmpty()) {
            // Пустые скобки — показываем placeholder для drop
            Box(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .width(40.dp)
                    .height(24.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(FormulaColors.parenthesesBorder.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "···",
                    color = FormulaColors.parenthesesSymbol.copy(alpha = 0.5f),
                    fontSize = (16 * scale).sp
                )
            }
        } else {
            // Рендерим содержимое
            FormulaRenderer(
                elements = parentheses.children,
                dragState = dragState,
                hoverState = hoverState,
                onDragStart = onDragStart,
                onDragEnd = onDragEnd,
                onDragMove = onDragMove,
                onEllipsisClick = onEllipsisClick,
                onVariableClick = onVariableClick,
                onParenthesesClick = onParenthesesClick,
                onOperatorClick = onOperatorClick,
                nestingLevel = nestingLevel + 1,
                variableValues = variableValues
            )
        }

        // Закрывающая скобка
        Text(
            text = ")",
            color = FormulaColors.parenthesesSymbol,
            fontSize = parenFontSize,
            fontWeight = FontWeight.Medium
        )
    }

    DisposableEffect(parentheses.id) {
        onDispose {
            boundsRegistry.unregister(parentheses.id)
        }
    }
}