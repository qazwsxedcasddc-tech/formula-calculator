package com.formulacalc.ui.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.formulacalc.model.FormulaToken
import com.formulacalc.model.engineeringFunctions
import com.formulacalc.model.engineeringOperators
import com.formulacalc.ui.draggableToken

/**
 * Вариант A: Классический научный калькулятор.
 *
 * Раскладка (сверху вниз):
 * 1. Научные функции: sin cos tan log ln √ ^ π
 * 2. Операторы и скобки: + − × ÷ ( )
 * 3. Цифры: 7 8 9 / 4 5 6 / 1 2 3 / 0 .
 * 4. Управление: C ⌫ = справа
 */
@Composable
fun ClassicCalculatorTab(
    onDigitClick: (String) -> Unit,
    onDecimalClick: () -> Unit,
    onClearClick: () -> Unit,
    onBackspaceClick: () -> Unit,
    onEqualsClick: () -> Unit,
    onTokenClick: (FormulaToken) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Ряд 1: Научные функции (sin, cos, tan, log, ln, √, ^, π)
        val functions = engineeringFunctions.take(8)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            functions.forEach { token ->
                FuncButton(
                    text = token.displayText,
                    token = token,
                    onClick = { onTokenClick(token) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Ряд 2: Операторы (+ − × ÷ ( ) ^ =)
        val operators = engineeringOperators.take(8)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            operators.forEach { token ->
                OpButton(
                    text = token.displayText,
                    token = token,
                    onClick = { onTokenClick(token) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Ряды 3-6: Цифровая клавиатура с C, ⌫, = справа
        // Ряд 3: 7 8 9 C
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.2f),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            NumButton("7", { onDigitClick("7") }, Modifier.weight(1f))
            NumButton("8", { onDigitClick("8") }, Modifier.weight(1f))
            NumButton("9", { onDigitClick("9") }, Modifier.weight(1f))
            ActionButton(
                text = "C",
                onClick = onClearClick,
                modifier = Modifier.weight(1f),
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        }

        // Ряд 4: 4 5 6 ⌫
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.2f),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            NumButton("4", { onDigitClick("4") }, Modifier.weight(1f))
            NumButton("5", { onDigitClick("5") }, Modifier.weight(1f))
            NumButton("6", { onDigitClick("6") }, Modifier.weight(1f))
            ActionButton(
                text = "⌫",
                onClick = onBackspaceClick,
                modifier = Modifier.weight(1f),
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }

        // Ряд 5: 1 2 3 =
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.2f),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            NumButton("1", { onDigitClick("1") }, Modifier.weight(1f))
            NumButton("2", { onDigitClick("2") }, Modifier.weight(1f))
            NumButton("3", { onDigitClick("3") }, Modifier.weight(1f))
            ActionButton(
                text = "=",
                onClick = onEqualsClick,
                modifier = Modifier.weight(1f),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }

        // Ряд 6: 0 (широкий) . (пустое место)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.2f),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            NumButton("0", { onDigitClick("0") }, Modifier.weight(2f))
            NumButton(".", onDecimalClick, Modifier.weight(1f))
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun NumButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        contentPadding = PaddingValues(4.dp)
    ) {
        Text(
            text = text,
            fontSize = 28.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color,
    contentColor: Color
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
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun OpButton(
    text: String,
    token: FormulaToken,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxHeight()
            .draggableToken(token, onClick),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        contentPadding = PaddingValues(2.dp)
    ) {
        Text(
            text = text,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun FuncButton(
    text: String,
    token: FormulaToken,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxHeight()
            .draggableToken(token, onClick),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        ),
        contentPadding = PaddingValues(2.dp)
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
