package com.formulacalc.ui.formula

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
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
 * Состояние drag для элемента
 */
data class DragState(
    val isDragging: Boolean = false,
    val draggedElement: FormulaElement? = null,
    val dragOffset: Offset = Offset.Zero
)

/**
 * Состояние hover для drop indicator
 */
data class HoverState(
    val targetId: String? = null,
    val side: DropSide? = null
)

/**
 * Рендеринг формулы
 */
@Composable
fun FormulaRenderer(
    elements: List<FormulaElement>,
    modifier: Modifier = Modifier,
    dragState: DragState = DragState(),
    hoverState: HoverState = HoverState(),
    onDragStart: (FormulaElement) -> Unit = {},
    onDragEnd: () -> Unit = {},
    onDrag: (Offset) -> Unit = {},
    onHover: (String?, DropSide?) -> Unit = { _, _ -> },
    onDrop: (FormulaElement, String, DropSide) -> Unit = { _, _, _ -> },
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
                onDrop = onDrop,
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
    onDragStart: (FormulaElement) -> Unit,
    onDragEnd: () -> Unit,
    onDrag: (Offset) -> Unit,
    onHover: (String?, DropSide?) -> Unit,
    onDrop: (FormulaElement, String, DropSide) -> Unit,
    onEllipsisClick: (String) -> Unit,
    onVariableClick: (String) -> Unit,
    nestingLevel: Int
) {
    val isDraggedElement = dragState.draggedElement?.id == element.id
    val opacity = if (isDraggedElement) 0.4f else 1f

    Box(
        modifier = Modifier.padding(horizontal = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        // Drop indicator слева
        if (hoverState.targetId == element.id && hoverState.side == DropSide.LEFT) {
            DropIndicator(isVertical = true, modifier = Modifier.align(Alignment.CenterStart))
        }

        // Drop indicator сверху
        if (hoverState.targetId == element.id && hoverState.side == DropSide.TOP) {
            DropIndicator(isVertical = false, modifier = Modifier.align(Alignment.TopCenter))
        }

        when (element) {
            is FormulaElement.Variable -> VariableView(
                variable = element,
                scale = scale,
                opacity = opacity,
                onClick = { onVariableClick(element.id) },
                onDragStart = { onDragStart(element) },
                onDragEnd = onDragEnd,
                onDrag = onDrag
            )

            is FormulaElement.Operator -> OperatorView(
                operator = element,
                scale = scale,
                opacity = opacity
            )

            is FormulaElement.Equals -> EqualsView(scale = scale, opacity = opacity)

            is FormulaElement.Ellipsis -> EllipsisView(
                ellipsis = element,
                scale = scale,
                opacity = opacity,
                onClick = { onEllipsisClick(element.id) }
            )

            is FormulaElement.Fraction -> FractionView(
                fraction = element,
                scale = scale,
                opacity = opacity,
                dragState = dragState,
                hoverState = hoverState,
                onDragStart = onDragStart,
                onDragEnd = onDragEnd,
                onDrag = onDrag,
                onHover = onHover,
                onDrop = onDrop,
                onEllipsisClick = onEllipsisClick,
                onVariableClick = onVariableClick,
                nestingLevel = nestingLevel
            )
        }

        // Drop indicator справа
        if (hoverState.targetId == element.id && hoverState.side == DropSide.RIGHT) {
            DropIndicator(isVertical = true, modifier = Modifier.align(Alignment.CenterEnd))
        }

        // Drop indicator снизу
        if (hoverState.targetId == element.id && hoverState.side == DropSide.BOTTOM) {
            DropIndicator(isVertical = false, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}

/**
 * Переменная
 */
@Composable
private fun VariableView(
    variable: FormulaElement.Variable,
    scale: Float,
    opacity: Float,
    onClick: () -> Unit,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    onDrag: (Offset) -> Unit
) {
    val fontSize = (28 * scale).sp
    val exponentSize = (16 * scale).sp
    val paddingH = (14 * scale).dp
    val paddingV = (8 * scale).dp

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        FormulaColors.variableGradientStart,
                        FormulaColors.variableGradientEnd
                    )
                ),
                alpha = opacity
            )
            .clickable { onClick() }
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { onDragStart() },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() },
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
}

/**
 * Оператор
 */
@Composable
private fun OperatorView(
    operator: FormulaElement.Operator,
    scale: Float,
    opacity: Float
) {
    Text(
        text = operator.symbol,
        color = FormulaColors.operatorColor.copy(alpha = opacity),
        fontSize = (28 * scale).sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

/**
 * Знак равенства
 */
@Composable
private fun EqualsView(scale: Float, opacity: Float) {
    Text(
        text = "=",
        color = FormulaColors.operatorColor.copy(alpha = opacity),
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
    ellipsis: FormulaElement.Ellipsis,
    scale: Float,
    opacity: Float,
    onClick: () -> Unit
) {
    Text(
        text = "···",
        color = FormulaColors.ellipsisText.copy(alpha = opacity),
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
                ),
                alpha = opacity
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
    opacity: Float,
    dragState: DragState,
    hoverState: HoverState,
    onDragStart: (FormulaElement) -> Unit,
    onDragEnd: () -> Unit,
    onDrag: (Offset) -> Unit,
    onHover: (String?, DropSide?) -> Unit,
    onDrop: (FormulaElement, String, DropSide) -> Unit,
    onEllipsisClick: (String) -> Unit,
    onVariableClick: (String) -> Unit,
    nestingLevel: Int
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(FormulaColors.fractionBackground.copy(alpha = opacity))
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
            onDrop = onDrop,
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
                    shape = RoundedCornerShape(2.dp),
                    alpha = opacity
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
            onDrop = onDrop,
            onEllipsisClick = onEllipsisClick,
            onVariableClick = onVariableClick,
            nestingLevel = nestingLevel + 1
        )
    }
}

/**
 * Индикатор места drop
 */
@Composable
private fun DropIndicator(
    isVertical: Boolean,
    modifier: Modifier = Modifier
) {
    val color = if (isVertical) FormulaColors.dropIndicatorGreen else FormulaColors.dropIndicatorPurple

    Box(
        modifier = modifier
            .then(
                if (isVertical) {
                    Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                } else {
                    Modifier
                        .height(4.dp)
                        .fillMaxWidth()
                }
            )
            .background(color, RoundedCornerShape(2.dp))
    )
}
