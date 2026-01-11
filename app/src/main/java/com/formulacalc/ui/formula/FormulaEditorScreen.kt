package com.formulacalc.ui.formula

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.formulacalc.model.PresetFormula
import com.formulacalc.ui.DragData
import com.formulacalc.ui.dropTarget
import com.formulacalc.viewmodel.FormulaEditorViewModel
import java.text.DecimalFormat

/**
 * Экран редактора формул с поддержкой drag & drop
 */
@Composable
fun FormulaEditorScreen(
    viewModel: FormulaEditorViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()

    // Предоставляем boundsRegistry через CompositionLocal
    CompositionLocalProvider(LocalElementBoundsRegistry provides viewModel.boundsRegistry) {
        Box(modifier = modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Компактный заголовок с кнопкой сброса
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Формула",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Кнопка сброса
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEE5A5A))
                            .clickable { viewModel.reset() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✕",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Область формулы — занимает всё доступное место
                FormulaArea(
                    elements = state.elements,
                    dragState = state.dragState,
                    hoverState = state.hoverState,
                    variableValues = state.variableValues,
                    onDragStart = { element, offset -> viewModel.onDragStart(element, offset) },
                    onDragEnd = { viewModel.onDragEnd() },
                    onDragMove = { viewModel.onDragMove(it) },
                    onEllipsisClick = { viewModel.onEllipsisClick(it) },
                    onVariableClick = { viewModel.onVariableClickForValue(it) }, // Для ввода значений
                    onPresetDrop = { preset -> viewModel.dropPreset(preset) },
                    modifier = Modifier.weight(1f)
                )

                // Результат вычисления
                state.calculationResult?.let { result ->
                    ResultDisplay(result = result)
                }

                // Компактные подсказки внизу
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Зелёная линия
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(12.dp)
                                .background(FormulaColors.dropIndicatorGreen, RoundedCornerShape(1.dp))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "рядом",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Фиолетовая линия
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .width(12.dp)
                                .height(3.dp)
                                .background(FormulaColors.dropIndicatorPurple, RoundedCornerShape(1.dp))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "дробь",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Tap
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "···",
                            color = FormulaColors.ellipsisText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "оператор",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Меню оператора
            if (state.showOperatorMenu) {
                OperatorMenu(
                    onSelect = { viewModel.selectOperator(it) },
                    onDismiss = { viewModel.dismissOperatorMenu() }
                )
            }

            // Клавиатура экспоненты
            if (state.showExponentKeyboard) {
                ExponentKeyboard(
                    currentExponent = state.currentExponent,
                    onSave = { viewModel.saveExponent(it) },
                    onDismiss = { viewModel.dismissExponentKeyboard() }
                )
            }

            // Диалог ввода значения переменной
            if (state.showVariableInput) {
                VariableInputDialog(
                    variableName = state.variableInputName,
                    currentValue = state.variableValues[state.variableInputName]?.let {
                        formatResultNumber(it)
                    } ?: "",
                    onValueChange = { /* не используется */ },
                    onDismiss = { viewModel.dismissVariableInput() },
                    onConfirm = { value ->
                        viewModel.setVariableValue(state.variableInputName, value)
                    }
                )
            }
        }
    }
}

/**
 * Форматирование числа для результата
 */
private fun formatResultNumber(value: Double): String {
    return if (value == value.toLong().toDouble() && kotlin.math.abs(value) < 1e10) {
        value.toLong().toString()
    } else {
        DecimalFormat("#.########").format(value)
    }
}

/**
 * Отображение результата вычисления
 */
@Composable
private fun ResultDisplay(result: Double) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF22C55E).copy(alpha = 0.1f),
                        Color(0xFF16A34A).copy(alpha = 0.1f)
                    )
                )
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "= ",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF22C55E)
            )
            Text(
                text = formatResultNumber(result),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF22C55E)
            )
        }
    }
}

/**
 * Область с формулой — поддерживает drop для preset формул
 * С горизонтальным скроллом и индикаторами
 */
@Composable
private fun FormulaArea(
    elements: List<com.formulacalc.model.FormulaElement>,
    dragState: DragState,
    hoverState: HoverState,
    variableValues: Map<String, Double>,
    onDragStart: (com.formulacalc.model.FormulaElement, androidx.compose.ui.geometry.Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragMove: (androidx.compose.ui.geometry.Offset) -> Unit,
    onEllipsisClick: (String) -> Unit,
    onVariableClick: (String) -> Unit,
    onPresetDrop: (PresetFormula) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragOver by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    // Анимация цвета границы при drag over
    val borderColor by animateColorAsState(
        targetValue = if (isDragOver) {
            FormulaColors.dropIndicatorGreen
        } else {
            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        },
        label = "borderColor"
    )

    val borderWidth = if (isDragOver) 2.dp else 1.dp

    // Проверяем, можно ли скроллить
    val canScrollLeft = scrollState.value > 0
    val canScrollRight = scrollState.value < scrollState.maxValue

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = borderWidth,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .background(
                if (isDragOver) {
                    FormulaColors.dropIndicatorGreen.copy(alpha = 0.1f)
                } else {
                    MaterialTheme.colorScheme.surface
                }
            )
            .dropTarget(
                onDragOver = { isDragOver = it },
                onDrop = { data ->
                    when (data) {
                        is DragData.Preset -> onPresetDrop(data.preset)
                        else -> { /* игнорируем другие типы */ }
                    }
                }
            )
    ) {
        // Контент с горизонтальным скроллом
        Box(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(scrollState)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (elements.isEmpty()) {
                Text(
                    text = if (isDragOver) "Отпустите формулу здесь" else "Перетащите формулу сюда",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isDragOver) {
                        FormulaColors.dropIndicatorGreen
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    }
                )
            } else {
                FormulaRenderer(
                    elements = elements,
                    dragState = dragState,
                    hoverState = hoverState,
                    onDragStart = onDragStart,
                    onDragEnd = onDragEnd,
                    onDragMove = onDragMove,
                    onEllipsisClick = onEllipsisClick,
                    onVariableClick = onVariableClick,
                    variableValues = variableValues
                )
            }
        }

        // Градиент слева — показывает что можно скроллить влево
        if (canScrollLeft) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(24.dp)
                    .fillMaxHeight()
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface,
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        // Градиент справа — показывает что можно скроллить вправо
        if (canScrollRight) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(24.dp)
                    .fillMaxHeight()
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
            )
        }
    }
}
