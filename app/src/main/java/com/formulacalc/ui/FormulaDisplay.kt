package com.formulacalc.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.formulacalc.model.Formula
import com.formulacalc.model.FormulaToken

/**
 * Зона A: Окно формулы.
 *
 * Отображает текущую формулу с курсором.
 * Поддерживает drag & drop (подсветка при перетаскивании).
 */
@Composable
fun FormulaDisplay(
    formula: Formula,
    result: String?,
    error: String?,
    isDragOver: Boolean,
    isResultDisplayed: Boolean,
    onCursorPositionChanged: (Int) -> Unit,
    onDragOver: (Boolean) -> Unit,
    onTokenDropped: (FormulaToken, Int?) -> Unit,
    onDrop: (DragData) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Анимация цвета границы при drag over
    val borderColor by animateColorAsState(
        targetValue = if (isDragOver) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        },
        animationSpec = tween(200),
        label = "borderColor"
    )

    // Анимация фона при drag over
    val backgroundColor by animateColorAsState(
        targetValue = if (isDragOver) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(200),
        label = "backgroundColor"
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(
                width = 2.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .dropTarget(
                onDragOver = onDragOver,
                onDrop = onDrop
            )
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Формула
        if (formula.isEmpty() && result == null) {
            // Placeholder
            Text(
                text = "Введите формулу",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                textAlign = TextAlign.Center
            )
        } else {
            // Отображение токенов с курсором
            FormulaTokensRow(
                formula = formula,
                onTokenClick = { index -> onCursorPositionChanged(index) }
            )
        }

        // Результат или ошибка
        if (result != null || error != null) {
            Spacer(modifier = Modifier.height(12.dp))

            if (error != null) {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            } else if (result != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "= ",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = result,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Строка токенов формулы с мигающим курсором
 */
@Composable
private fun FormulaTokensRow(
    formula: Formula,
    onTokenClick: (Int) -> Unit
) {
    // Мигающий курсор
    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorAlpha"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Курсор в начале (если позиция = 0)
        if (formula.cursorPosition == 0) {
            Cursor(alpha = cursorAlpha)
        }

        formula.tokens.forEachIndexed { index, token ->
            // Токен
            TokenView(
                token = token,
                onClick = { onTokenClick(index) }
            )

            // Курсор после токена
            if (formula.cursorPosition == index + 1) {
                Cursor(alpha = cursorAlpha)
            }
        }
    }
}

/**
 * Отображение одного токена
 */
@Composable
private fun TokenView(
    token: FormulaToken,
    onClick: () -> Unit
) {
    val textColor = when (token) {
        is FormulaToken.Number -> MaterialTheme.colorScheme.onSurface
        is FormulaToken.Operator -> MaterialTheme.colorScheme.primary
        is FormulaToken.Function -> MaterialTheme.colorScheme.tertiary
        is FormulaToken.Variable -> MaterialTheme.colorScheme.secondary
        is FormulaToken.Parenthesis -> MaterialTheme.colorScheme.onSurface
        is FormulaToken.Subscript -> MaterialTheme.colorScheme.secondary
        is FormulaToken.Superscript -> MaterialTheme.colorScheme.secondary
    }

    val fontStyle = when (token) {
        is FormulaToken.Variable -> FontStyle.Italic
        is FormulaToken.Subscript -> FontStyle.Italic
        is FormulaToken.Superscript -> FontStyle.Italic
        else -> FontStyle.Normal
    }

    Text(
        text = token.displayText,
        style = MaterialTheme.typography.headlineMedium,
        color = textColor,
        fontStyle = fontStyle,
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 2.dp)
    )
}

/**
 * Мигающий курсор
 */
@Composable
private fun Cursor(alpha: Float) {
    Box(
        modifier = Modifier
            .width(2.dp)
            .height(32.dp)
            .background(
                MaterialTheme.colorScheme.primary.copy(alpha = alpha)
            )
    )
}
