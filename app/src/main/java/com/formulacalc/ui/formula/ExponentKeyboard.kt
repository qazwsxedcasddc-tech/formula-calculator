package com.formulacalc.ui.formula

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.formulacalc.model.Exponent

/**
 * Режим ввода экспоненты
 */
enum class ExponentMode {
    SIMPLE,     // xⁿ
    FRACTION    // x^(a/b)
}

/**
 * Клавиатура для ввода экспоненты
 */
@Composable
fun ExponentKeyboard(
    currentExponent: Exponent?,
    onSave: (Exponent?) -> Unit,
    onDismiss: () -> Unit
) {
    var mode by remember { mutableStateOf(ExponentMode.SIMPLE) }
    var simpleValue by remember {
        mutableStateOf(
            when (currentExponent) {
                is Exponent.Simple -> currentExponent.value
                else -> ""
            }
        )
    }
    var numerator by remember {
        mutableStateOf(
            when (currentExponent) {
                is Exponent.Fraction -> currentExponent.numerator
                else -> ""
            }
        )
    }
    var denominator by remember {
        mutableStateOf(
            when (currentExponent) {
                is Exponent.Fraction -> currentExponent.denominator
                else -> ""
            }
        )
    }
    var isEditingNumerator by remember { mutableStateOf(true) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(24.dp, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            color = Color.White
        ) {
            Column {
                // Заголовок
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color(0xFF3B82F6), Color(0xFF2563EB))
                            )
                        )
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Степень",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Переключатель режима
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ModeTab(
                        text = "xⁿ",
                        isSelected = mode == ExponentMode.SIMPLE,
                        onClick = { mode = ExponentMode.SIMPLE }
                    )
                    ModeTab(
                        text = "x^(a/b)",
                        isSelected = mode == ExponentMode.FRACTION,
                        onClick = { mode = ExponentMode.FRACTION }
                    )
                }

                // Превью
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF8FAFC)),
                    contentAlignment = Alignment.Center
                ) {
                    when (mode) {
                        ExponentMode.SIMPLE -> {
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(SpanStyle(color = Color(0xFF3B82F6), fontSize = 32.sp)) {
                                        append("x")
                                    }
                                    withStyle(
                                        SpanStyle(
                                            color = Color(0xFFEF4444),
                                            fontSize = 20.sp,
                                            baselineShift = BaselineShift.Superscript
                                        )
                                    ) {
                                        append(simpleValue.ifEmpty { "n" })
                                    }
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                        ExponentMode.FRACTION -> {
                            Row(verticalAlignment = Alignment.Top) {
                                Text(
                                    text = "x",
                                    color = Color(0xFF3B82F6),
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(start = 4.dp)
                                ) {
                                    Text(
                                        text = numerator.ifEmpty { "a" },
                                        color = if (isEditingNumerator) Color(0xFFEF4444) else Color(0xFF6B7280),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Box(
                                        modifier = Modifier
                                            .width(24.dp)
                                            .height(2.dp)
                                            .background(Color(0xFF9CA3AF))
                                    )
                                    Text(
                                        text = denominator.ifEmpty { "b" },
                                        color = if (!isEditingNumerator) Color(0xFFEF4444) else Color(0xFF6B7280),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                // Клавиатура
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Цифры
                    items(listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")) { key ->
                        KeyButton(
                            text = key,
                            onClick = {
                                when (mode) {
                                    ExponentMode.SIMPLE -> simpleValue += key
                                    ExponentMode.FRACTION -> {
                                        if (isEditingNumerator) numerator += key
                                        else denominator += key
                                    }
                                }
                            }
                        )
                    }

                    // Буквы (первые 15)
                    items(listOf("a", "b", "c", "n", "x")) { key ->
                        KeyButton(
                            text = key,
                            onClick = {
                                when (mode) {
                                    ExponentMode.SIMPLE -> simpleValue += key
                                    ExponentMode.FRACTION -> {
                                        if (isEditingNumerator) numerator += key
                                        else denominator += key
                                    }
                                }
                            }
                        )
                    }

                    // Специальные клавиши
                    item {
                        KeyButton(
                            text = "−",
                            onClick = {
                                when (mode) {
                                    ExponentMode.SIMPLE -> simpleValue += "-"
                                    ExponentMode.FRACTION -> {
                                        if (isEditingNumerator) numerator += "-"
                                        else denominator += "-"
                                    }
                                }
                            }
                        )
                    }
                    item {
                        KeyButton(
                            text = "+",
                            onClick = {
                                when (mode) {
                                    ExponentMode.SIMPLE -> simpleValue += "+"
                                    ExponentMode.FRACTION -> {
                                        if (isEditingNumerator) numerator += "+"
                                        else denominator += "+"
                                    }
                                }
                            }
                        )
                    }

                    // Переключение числитель/знаменатель (только для дробного режима)
                    if (mode == ExponentMode.FRACTION) {
                        item {
                            KeyButton(
                                text = "↕",
                                onClick = { isEditingNumerator = !isEditingNumerator },
                                isAction = true
                            )
                        }
                    }

                    // Backspace
                    item {
                        KeyButton(
                            text = "⌫",
                            onClick = {
                                when (mode) {
                                    ExponentMode.SIMPLE -> {
                                        if (simpleValue.isNotEmpty()) {
                                            simpleValue = simpleValue.dropLast(1)
                                        }
                                    }
                                    ExponentMode.FRACTION -> {
                                        if (isEditingNumerator && numerator.isNotEmpty()) {
                                            numerator = numerator.dropLast(1)
                                        } else if (!isEditingNumerator && denominator.isNotEmpty()) {
                                            denominator = denominator.dropLast(1)
                                        }
                                    }
                                }
                            },
                            isAction = true
                        )
                    }

                    // Clear
                    item {
                        KeyButton(
                            text = "C",
                            onClick = {
                                simpleValue = ""
                                numerator = ""
                                denominator = ""
                            },
                            isAction = true
                        )
                    }
                }

                // Кнопки действий
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Удалить степень
                    ActionButton(
                        text = "Удалить",
                        modifier = Modifier.weight(1f),
                        isDestructive = true,
                        onClick = { onSave(null) }
                    )

                    // Отмена
                    ActionButton(
                        text = "Отмена",
                        modifier = Modifier.weight(1f),
                        onClick = onDismiss
                    )

                    // Сохранить
                    ActionButton(
                        text = "Сохранить",
                        modifier = Modifier.weight(1f),
                        isPrimary = true,
                        onClick = {
                            val exponent = when (mode) {
                                ExponentMode.SIMPLE -> {
                                    if (simpleValue.isNotEmpty()) Exponent.Simple(simpleValue)
                                    else null
                                }
                                ExponentMode.FRACTION -> {
                                    if (numerator.isNotEmpty() && denominator.isNotEmpty()) {
                                        Exponent.Fraction(numerator, denominator)
                                    } else null
                                }
                            }
                            onSave(exponent)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ModeTab(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) {
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xFF3B82F6), Color(0xFF2563EB))
                    )
                } else {
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xFFE2E8F0), Color(0xFFE2E8F0))
                    )
                }
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.White else Color(0xFF64748B),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun KeyButton(
    text: String,
    onClick: () -> Unit,
    isAction: Boolean = false
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isAction) {
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFFFEE2E2), Color(0xFFFECACA))
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFFF1F5F9), Color(0xFFE2E8F0))
                    )
                }
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = if (isAction) Color(0xFFDC2626) else Color(0xFF374151)
        )
    }
}

@Composable
private fun ActionButton(
    text: String,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    isPrimary -> Brush.horizontalGradient(
                        colors = listOf(Color(0xFF22C55E), Color(0xFF16A34A))
                    )
                    isDestructive -> Brush.horizontalGradient(
                        colors = listOf(Color(0xFFEF4444), Color(0xFFDC2626))
                    )
                    else -> Brush.horizontalGradient(
                        colors = listOf(Color(0xFFE2E8F0), Color(0xFFE2E8F0))
                    )
                }
            )
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isPrimary || isDestructive) Color.White else Color(0xFF64748B),
            fontWeight = FontWeight.SemiBold
        )
    }
}
