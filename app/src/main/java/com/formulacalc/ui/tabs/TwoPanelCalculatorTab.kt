package com.formulacalc.ui.tabs

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.formulacalc.model.FormulaToken
import com.formulacalc.model.PresetFormula
import com.formulacalc.model.engineeringFunctions
import com.formulacalc.model.engineeringOperators
import com.formulacalc.model.greekSymbols
import com.formulacalc.model.presetFormulas
import com.formulacalc.ui.draggableToken
import kotlinx.coroutines.launch

/**
 * Вариант B: Двухпанельный калькулятор со свайпом.
 *
 * - Левая панель (фиксированная): цифры + базовые операторы
 * - Правая панель (свайпается): функции / греческие / формулы
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TwoPanelCalculatorTab(
    onDigitClick: (String) -> Unit,
    onDecimalClick: () -> Unit,
    onClearClick: () -> Unit,
    onBackspaceClick: () -> Unit,
    onEqualsClick: () -> Unit,
    onTokenClick: (FormulaToken) -> Unit,
    onPresetClick: (PresetFormula) -> Unit,
    onPresetDoubleTap: (PresetFormula) -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    Row(
        modifier = modifier.padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Левая панель: Цифры и базовые операторы (50%)
        Column(
            modifier = Modifier
                .weight(0.5f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Ряд 1: 7 8 9 ÷
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                NumBtn("7", { onDigitClick("7") }, Modifier.weight(1f))
                NumBtn("8", { onDigitClick("8") }, Modifier.weight(1f))
                NumBtn("9", { onDigitClick("9") }, Modifier.weight(1f))
                OpBtn("÷", engineeringOperators.find { it.displayText == "÷" }!!, onTokenClick, Modifier.weight(1f))
            }

            // Ряд 2: 4 5 6 ×
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                NumBtn("4", { onDigitClick("4") }, Modifier.weight(1f))
                NumBtn("5", { onDigitClick("5") }, Modifier.weight(1f))
                NumBtn("6", { onDigitClick("6") }, Modifier.weight(1f))
                OpBtn("×", engineeringOperators.find { it.displayText == "×" }!!, onTokenClick, Modifier.weight(1f))
            }

            // Ряд 3: 1 2 3 −
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                NumBtn("1", { onDigitClick("1") }, Modifier.weight(1f))
                NumBtn("2", { onDigitClick("2") }, Modifier.weight(1f))
                NumBtn("3", { onDigitClick("3") }, Modifier.weight(1f))
                OpBtn("−", engineeringOperators.find { it.displayText == "−" }!!, onTokenClick, Modifier.weight(1f))
            }

            // Ряд 4: 0 . = +
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                NumBtn("0", { onDigitClick("0") }, Modifier.weight(1f))
                NumBtn(".", onDecimalClick, Modifier.weight(1f))
                ActBtn("=", onEqualsClick, Modifier.weight(1f), MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary)
                OpBtn("+", engineeringOperators.find { it.displayText == "+" }!!, onTokenClick, Modifier.weight(1f))
            }

            // Ряд 5: C ⌫ ( )
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ActBtn("C", onClearClick, Modifier.weight(1f), MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
                ActBtn("⌫", onBackspaceClick, Modifier.weight(1f), MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
                OpBtn("(", engineeringOperators.find { it.displayText == "(" }!!, onTokenClick, Modifier.weight(1f))
                OpBtn(")", engineeringOperators.find { it.displayText == ")" }!!, onTokenClick, Modifier.weight(1f))
            }
        }

        // Правая панель: Свайпаемая (50%)
        Column(
            modifier = Modifier
                .weight(0.5f)
                .fillMaxHeight()
        ) {
            // Индикаторы страниц
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                listOf("Функции", "Греч.", "Формулы").forEachIndexed { index, title ->
                    TextButton(
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(2.dp)
                    ) {
                        Text(
                            text = title,
                            fontSize = 10.sp,
                            color = if (pagerState.currentPage == index)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            // Pager со страницами
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> FunctionsPage(onTokenClick)
                    1 -> GreekPage(onTokenClick)
                    2 -> FormulasPage(onPresetDoubleTap)
                }
            }
        }
    }
}

@Composable
private fun FunctionsPage(onTokenClick: (FormulaToken) -> Unit) {
    val functions = engineeringFunctions + engineeringOperators.filter { it.displayText == "^" }
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        functions.chunked(4).take(4).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                row.forEach { token ->
                    FuncBtn(token.displayText, token, onTokenClick, Modifier.weight(1f))
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun GreekPage(onTokenClick: (FormulaToken) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        greekSymbols.chunked(4).take(4).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                row.forEach { token ->
                    GreekBtn(token.displayText, token, onTokenClick, Modifier.weight(1f))
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun FormulasPage(onPresetDoubleTap: (PresetFormula) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        presetFormulas.take(4).forEach { preset ->
            Card(
                onClick = { onPresetDoubleTap(preset) },
                modifier = Modifier.fillMaxWidth().weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(preset.name, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(preset.toDisplayString(), fontSize = 14.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun NumBtn(text: String, onClick: () -> Unit, modifier: Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        contentPadding = PaddingValues(2.dp)
    ) {
        Text(text, fontSize = 22.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun OpBtn(text: String, token: FormulaToken, onTokenClick: (FormulaToken) -> Unit, modifier: Modifier) {
    Button(
        onClick = { onTokenClick(token) },
        modifier = modifier.fillMaxHeight().draggableToken(token) { onTokenClick(token) },
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        contentPadding = PaddingValues(2.dp)
    ) {
        Text(text, fontSize = 20.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ActBtn(text: String, onClick: () -> Unit, modifier: Modifier, containerColor: Color, contentColor: Color) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = contentColor),
        contentPadding = PaddingValues(2.dp)
    ) {
        Text(text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun FuncBtn(text: String, token: FormulaToken, onTokenClick: (FormulaToken) -> Unit, modifier: Modifier) {
    Button(
        onClick = { onTokenClick(token) },
        modifier = modifier.fillMaxHeight().draggableToken(token) { onTokenClick(token) },
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        ),
        contentPadding = PaddingValues(2.dp)
    ) {
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
    }
}

@Composable
private fun GreekBtn(text: String, token: FormulaToken, onTokenClick: (FormulaToken) -> Unit, modifier: Modifier) {
    Button(
        onClick = { onTokenClick(token) },
        modifier = modifier.fillMaxHeight().draggableToken(token) { onTokenClick(token) },
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ),
        contentPadding = PaddingValues(2.dp)
    ) {
        Text(text, fontSize = 18.sp)
    }
}
