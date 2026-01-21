package com.formulacalc.ui.tabs

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.formulacalc.model.FormulaToken
import com.formulacalc.model.PresetFormula
import com.formulacalc.model.engineeringFunctions
import com.formulacalc.model.engineeringOperators
import com.formulacalc.model.greekSymbols
import com.formulacalc.model.presetFormulas
import com.formulacalc.ui.draggableToken

/**
 * Вариант C: Калькулятор с выдвижными панелями.
 *
 * - Основной экран: базовый калькулятор
 * - Свайп влево: выдвигается панель с научными функциями
 * - Свайп вправо: выдвигается панель с формулами/греческими
 */
@Composable
fun DrawerCalculatorTab(
    onDigitClick: (String) -> Unit,
    onDecimalClick: () -> Unit,
    onClearClick: () -> Unit,
    onBackspaceClick: () -> Unit,
    onEqualsClick: () -> Unit,
    onTokenClick: (FormulaToken) -> Unit,
    onPresetDoubleTap: (PresetFormula) -> Unit,
    modifier: Modifier = Modifier
) {
    var leftDrawerOpen by remember { mutableStateOf(false) }
    var rightDrawerOpen by remember { mutableStateOf(false) }

    val leftDrawerWidth by animateDpAsState(
        targetValue = if (leftDrawerOpen) 140.dp else 0.dp,
        label = "leftDrawer"
    )
    val rightDrawerWidth by animateDpAsState(
        targetValue = if (rightDrawerOpen) 140.dp else 0.dp,
        label = "rightDrawer"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = { },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        when {
                            dragAmount > 20 -> {
                                if (leftDrawerOpen) leftDrawerOpen = false
                                else rightDrawerOpen = true
                            }
                            dragAmount < -20 -> {
                                if (rightDrawerOpen) rightDrawerOpen = false
                                else leftDrawerOpen = true
                            }
                        }
                    }
                )
            }
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Левая выдвижная панель: Функции
            if (leftDrawerWidth > 0.dp) {
                Column(
                    modifier = Modifier
                        .width(leftDrawerWidth)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "Функции",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(4.dp)
                    )
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(engineeringFunctions) { token ->
                            SmallFuncButton(token, onTokenClick)
                        }
                    }
                }
            }

            // Основной калькулятор
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Индикаторы свайпа
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = { leftDrawerOpen = !leftDrawerOpen }, modifier = Modifier.size(24.dp)) {
                        Icon(
                            if (leftDrawerOpen) Icons.Default.ChevronLeft else Icons.Default.ChevronRight,
                            contentDescription = "Functions",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                    }
                    Text(
                        "← Функции | Формулы →",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    IconButton(onClick = { rightDrawerOpen = !rightDrawerOpen }, modifier = Modifier.size(24.dp)) {
                        Icon(
                            if (rightDrawerOpen) Icons.Default.ChevronRight else Icons.Default.ChevronLeft,
                            contentDescription = "Formulas",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                    }
                }

                // Операторы
                Row(
                    modifier = Modifier.fillMaxWidth().weight(0.8f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    engineeringOperators.take(8).forEach { token ->
                        MediumOpButton(token, onTokenClick, Modifier.weight(1f))
                    }
                }

                // Цифровая клавиатура
                // Ряд 7 8 9 C
                Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    MainNumButton("7", { onDigitClick("7") }, Modifier.weight(1f))
                    MainNumButton("8", { onDigitClick("8") }, Modifier.weight(1f))
                    MainNumButton("9", { onDigitClick("9") }, Modifier.weight(1f))
                    MainActionButton("C", onClearClick, Modifier.weight(1f), MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
                }

                // Ряд 4 5 6 ⌫
                Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    MainNumButton("4", { onDigitClick("4") }, Modifier.weight(1f))
                    MainNumButton("5", { onDigitClick("5") }, Modifier.weight(1f))
                    MainNumButton("6", { onDigitClick("6") }, Modifier.weight(1f))
                    MainActionButton("⌫", onBackspaceClick, Modifier.weight(1f), MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
                }

                // Ряд 1 2 3 =
                Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    MainNumButton("1", { onDigitClick("1") }, Modifier.weight(1f))
                    MainNumButton("2", { onDigitClick("2") }, Modifier.weight(1f))
                    MainNumButton("3", { onDigitClick("3") }, Modifier.weight(1f))
                    MainActionButton("=", onEqualsClick, Modifier.weight(1f), MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary)
                }

                // Ряд 0 . (пустое)
                Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    MainNumButton("0", { onDigitClick("0") }, Modifier.weight(2f))
                    MainNumButton(".", onDecimalClick, Modifier.weight(1f))
                    Spacer(Modifier.weight(1f))
                }
            }

            // Правая выдвижная панель: Греческие и Формулы
            if (rightDrawerWidth > 0.dp) {
                Column(
                    modifier = Modifier
                        .width(rightDrawerWidth)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "Греческие",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(4.dp)
                    )
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.weight(0.4f),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(greekSymbols.take(12)) { token ->
                            SmallGreekButton(token, onTokenClick)
                        }
                    }

                    Divider()

                    Text(
                        "Формулы",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(4.dp)
                    )
                    LazyColumn(
                        modifier = Modifier.weight(0.6f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(presetFormulas.take(5)) { preset ->
                            Card(
                                onClick = { onPresetDoubleTap(preset) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    preset.toDisplayString(),
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SmallFuncButton(token: FormulaToken, onTokenClick: (FormulaToken) -> Unit) {
    Button(
        onClick = { onTokenClick(token) },
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.5f)
            .draggableToken(token) { onTokenClick(token) },
        shape = RoundedCornerShape(6.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        ),
        contentPadding = PaddingValues(2.dp)
    ) {
        Text(token.displayText, fontSize = 12.sp)
    }
}

@Composable
private fun SmallGreekButton(token: FormulaToken, onTokenClick: (FormulaToken) -> Unit) {
    Button(
        onClick = { onTokenClick(token) },
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .draggableToken(token) { onTokenClick(token) },
        shape = RoundedCornerShape(4.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ),
        contentPadding = PaddingValues(1.dp)
    ) {
        Text(token.displayText, fontSize = 14.sp)
    }
}

@Composable
private fun MediumOpButton(token: FormulaToken, onTokenClick: (FormulaToken) -> Unit, modifier: Modifier) {
    Button(
        onClick = { onTokenClick(token) },
        modifier = modifier.fillMaxHeight().draggableToken(token) { onTokenClick(token) },
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        contentPadding = PaddingValues(2.dp)
    ) {
        Text(token.displayText, fontSize = 16.sp)
    }
}

@Composable
private fun MainNumButton(text: String, onClick: () -> Unit, modifier: Modifier) {
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
        Text(text, fontSize = 26.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun MainActionButton(text: String, onClick: () -> Unit, modifier: Modifier, containerColor: Color, contentColor: Color) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = contentColor),
        contentPadding = PaddingValues(4.dp)
    ) {
        Text(text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}
