package com.formulacalc.ui.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.formulacalc.model.FormulaToken
import com.formulacalc.model.engineeringFunctions
import com.formulacalc.model.engineeringOperators
import com.formulacalc.ui.draggableToken

/**
 * Объединённая вкладка "Калькулятор".
 *
 * Содержит:
 * - Левая часть: Цифры 0-9, точка, C, ⌫, =
 * - Правая часть: Операторы + − × ÷ ^ ( ) и функции sin cos tan и т.д.
 */
@Composable
fun CombinedCalculatorTab(
    onDigitClick: (String) -> Unit,
    onDecimalClick: () -> Unit,
    onClearClick: () -> Unit,
    onBackspaceClick: () -> Unit,
    onEqualsClick: () -> Unit,
    onTokenClick: (FormulaToken) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Левая часть: Цифровая клавиатура (60%)
        Column(
            modifier = Modifier
                .weight(0.6f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Строка 1: 7 8 9 C
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CalcButton("7", { onDigitClick("7") }, Modifier.weight(1f))
                CalcButton("8", { onDigitClick("8") }, Modifier.weight(1f))
                CalcButton("9", { onDigitClick("9") }, Modifier.weight(1f))
                CalcButton(
                    "C", onClearClick, Modifier.weight(1f),
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            }

            // Строка 2: 4 5 6 ⌫
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CalcButton("4", { onDigitClick("4") }, Modifier.weight(1f))
                CalcButton("5", { onDigitClick("5") }, Modifier.weight(1f))
                CalcButton("6", { onDigitClick("6") }, Modifier.weight(1f))
                CalcButton(
                    "⌫", onBackspaceClick, Modifier.weight(1f),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            // Строка 3: 1 2 3 =
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CalcButton("1", { onDigitClick("1") }, Modifier.weight(1f))
                CalcButton("2", { onDigitClick("2") }, Modifier.weight(1f))
                CalcButton("3", { onDigitClick("3") }, Modifier.weight(1f))
                CalcButton(
                    "=", onEqualsClick, Modifier.weight(1f),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // Строка 4: 0 (широкий) .
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CalcButton("0", { onDigitClick("0") }, Modifier.weight(2f))
                CalcButton(".", onDecimalClick, Modifier.weight(1f))
                Spacer(modifier = Modifier.weight(1f))
            }
        }

        // Правая часть: Операторы и функции (40%)
        Column(
            modifier = Modifier
                .weight(0.4f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Операторы (2 ряда по 4)
            val operators = engineeringOperators.take(8)
            operators.chunked(4).forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    row.forEach { token ->
                        EngButton(
                            text = token.displayText,
                            token = token,
                            onClick = { onTokenClick(token) },
                            modifier = Modifier.weight(1f),
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    // Заполняем пустые ячейки если ряд неполный
                    repeat(4 - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            // Функции (2 ряда по 4)
            val functions = engineeringFunctions.take(8)
            functions.chunked(4).forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    row.forEach { token ->
                        EngButton(
                            text = token.displayText,
                            token = token,
                            onClick = { onTokenClick(token) },
                            modifier = Modifier.weight(1f),
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                    repeat(4 - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun CalcButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        contentPadding = PaddingValues(4.dp)
    ) {
        Text(
            text = text,
            fontSize = 24.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun EngButton(
    text: String,
    token: FormulaToken,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxHeight()
            .draggableToken(token, onClick),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        contentPadding = PaddingValues(2.dp)
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}
