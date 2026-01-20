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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.formulacalc.model.PresetFormula
import com.formulacalc.ui.DragData
import com.formulacalc.ui.dropTarget
import com.formulacalc.viewmodel.FormulaEditorViewModel
import com.formulacalc.util.AppLogger
import com.formulacalc.util.CalculationEntry
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import java.text.DecimalFormat
import android.content.Intent
import android.widget.Toast
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import kotlinx.coroutines.launch

/**
 * Экран редактора формул с поддержкой drag & drop
 */
@Composable
fun FormulaEditorScreen(
    viewModel: FormulaEditorViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Показываем snackbar при удалении
    LaunchedEffect(state.showDeleteSnackbar) {
        if (state.showDeleteSnackbar) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = "Удалено: ${state.deletedElementName}",
                    actionLabel = "Отменить"
                ).let { result ->
                    if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                        viewModel.undo()
                    }
                }
                viewModel.dismissDeleteSnackbar()
            }
        }
    }

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

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Кнопка Undo
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    if (state.canUndo) Color(0xFF3B82F6) else Color(0xFFE2E8F0)
                                )
                                .clickable(enabled = state.canUndo) { viewModel.undo() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "↩",
                                fontSize = 16.sp,
                                color = if (state.canUndo) Color.White else Color(0xFFA0AEC0)
                            )
                        }

                        // Кнопка Redo
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    if (state.canRedo) Color(0xFF3B82F6) else Color(0xFFE2E8F0)
                                )
                                .clickable(enabled = state.canRedo) { viewModel.redo() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "↪",
                                fontSize = 16.sp,
                                color = if (state.canRedo) Color.White else Color(0xFFA0AEC0)
                            )
                        }

                        // Кнопка истории
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    if (state.showHistoryPanel) Color(0xFF22C55E) else Color(0xFF94A3B8)
                                )
                                .clickable { viewModel.toggleHistoryPanel() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "📊",
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Кнопка копирования логов (для отладки)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF6366F1))
                                .clickable {
                                    val logs = AppLogger.getLogsAsString()
                                    clipboardManager.setText(AnnotatedString(logs))
                                    Toast.makeText(context, "Логи скопированы!", Toast.LENGTH_SHORT).show()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "📋",
                                fontSize = 14.sp
                            )
                        }

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
                    onParenthesesClick = { viewModel.onParenthesesClick(it) }, // Для скобок
                    onOperatorClick = { viewModel.onOperatorClick(it) }, // Для изменения оператора
                    onPresetDrop = { preset -> viewModel.dropPreset(preset) },
                    boundsRegistry = viewModel.boundsRegistry,
                    modifier = Modifier.weight(1f)
                )

                // Результат вычисления с кнопкой поделиться
                state.calculationResult?.let { result ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ResultDisplay(
                            result = result,
                            modifier = Modifier.weight(1f)
                        )

                        // Кнопка поделиться
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF3B82F6))
                                .clickable {
                                    val shareText = "Результат: ${formatResultNumber(result)}"
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Поделиться результатом"))
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "📤",
                                fontSize = 20.sp
                            )
                        }
                    }
                }

                // Панель истории
                if (state.showHistoryPanel && state.calculationHistory.isNotEmpty()) {
                    HistoryPanel(
                        history = state.calculationHistory,
                        onClear = { viewModel.clearHistory() }
                    )
                }

                // Зона удаления — показывается только при drag
                if (state.dragState.isDragging) {
                    DeleteZone(
                        isOutsideFormulaArea = !viewModel.boundsRegistry.isInsideFormulaArea(
                            state.dragState.fingerPosition,
                            margin = 100f
                        )
                    )
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

            // Меню оператора (для вставки нового или замены существующего)
            if (state.showOperatorMenu) {
                OperatorMenu(
                    onSelect = {
                        if (state.isOperatorReplaceMode) {
                            viewModel.replaceOperator(it)
                        } else {
                            viewModel.selectOperator(it)
                        }
                    },
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
            val targetId = state.variableInputTargetId
            if (state.showVariableInput && targetId != null) {
                val isConstant = isKnownConstant(state.variableInputName)
                val constantValue = getConstantDefaultValue(state.variableInputName)

                VariableInputDialog(
                    variableName = state.variableInputName,
                    currentValue = state.variableValues[targetId]?.let {
                        formatResultNumber(it)
                    } ?: "",
                    onValueChange = { /* не используется */ },
                    onDismiss = { viewModel.dismissVariableInput() },
                    onConfirm = { value ->
                        // Используем ID переменной вместо имени
                        viewModel.setVariableValue(targetId, value)
                    },
                    isConstant = isConstant,
                    constantDefaultValue = constantValue,
                    onWrapInParentheses = {
                        viewModel.wrapInParentheses(targetId)
                    }
                )
            }

            // Диалог для скобок
            val parenTargetId = state.parenthesesDialogTargetId
            if (state.showParenthesesDialog && parenTargetId != null) {
                ParenthesesDialog(
                    onDismiss = { viewModel.dismissParenthesesDialog() },
                    onUnwrap = { viewModel.unwrapParentheses(parenTargetId) }
                )
            }

            // Snackbar для отмены удаления
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
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
 * Проверить, является ли имя константой
 */
private fun isKnownConstant(name: String): Boolean {
    return name in listOf("π", "e", "c", "G", "φ")
}

/**
 * Получить значение константы по умолчанию
 */
private fun getConstantDefaultValue(name: String): Double? {
    return when (name) {
        "π" -> Math.PI
        "e" -> Math.E
        "c" -> 299792458.0
        "G" -> 6.67430e-11
        "φ" -> 1.618033988749895
        else -> null
    }
}

/**
 * Панель истории вычислений
 */
@Composable
private fun HistoryPanel(
    history: List<CalculationEntry>,
    onClear: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .heightIn(max = 150.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF1F5F9))
    ) {
        // Заголовок
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "История (${history.size})",
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                color = Color(0xFF64748B)
            )
            Text(
                text = "Очистить",
                fontSize = 12.sp,
                color = Color(0xFFEF4444),
                modifier = Modifier.clickable { onClear() }
            )
        }

        // Список
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            items(history.take(10)) { entry ->
                HistoryItem(entry = entry)
            }
        }
    }
}

/**
 * Элемент истории
 */
@Composable
private fun HistoryItem(entry: CalculationEntry) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                clipboardManager.setText(AnnotatedString(entry.getFormattedResult()))
                Toast.makeText(context, "Скопировано: ${entry.getFormattedResult()}", Toast.LENGTH_SHORT).show()
            }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "= ${entry.getFormattedResult()}",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFF22C55E)
            )
        }
        Text(
            text = entry.getFormattedTime(),
            fontSize = 10.sp,
            color = Color(0xFF94A3B8)
        )
    }
}

/**
 * Зона удаления — показывается внизу при перетаскивании
 */
@Composable
private fun DeleteZone(isOutsideFormulaArea: Boolean) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isOutsideFormulaArea) {
            Color(0xFFEF4444) // Красный когда элемент за пределами
        } else {
            Color(0xFFEF4444).copy(alpha = 0.3f) // Прозрачный красный
        },
        label = "deleteZoneColor"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "🗑️",
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isOutsideFormulaArea) "Отпустите для удаления" else "Перетащите сюда для удаления",
                color = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )
        }
    }
}

/**
 * Отображение результата вычисления — компактный блок с горизонтальным скроллом
 * Клик копирует результат в буфер обмена
 */
@Composable
private fun ResultDisplay(
    result: Double,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val resultText = formatResultNumber(result)

    Box(
        modifier = modifier
            .height(48.dp) // Фиксированная высота
            .clip(RoundedCornerShape(12.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF22C55E).copy(alpha = 0.15f),
                        Color(0xFF16A34A).copy(alpha = 0.15f)
                    )
                )
            )
            .clickable {
                clipboardManager.setText(AnnotatedString(resultText))
                Toast.makeText(context, "Скопировано: $resultText", Toast.LENGTH_SHORT).show()
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(scrollState)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "= ",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF22C55E)
                )
                Text(
                    text = resultText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF22C55E),
                    maxLines = 1
                )
            }

            // Иконка копирования
            Text(
                text = "📋",
                fontSize = 14.sp,
                modifier = Modifier.padding(start = 8.dp)
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
    onParenthesesClick: (String) -> Unit,
    onOperatorClick: (String) -> Unit,
    onPresetDrop: (PresetFormula) -> Unit,
    boundsRegistry: ElementBoundsRegistry,
    modifier: Modifier = Modifier
) {
    var isDragOver by remember { mutableStateOf(false) }

    // Для автомасштабирования
    var containerWidth by remember { mutableStateOf(0f) }
    var containerHeight by remember { mutableStateOf(0f) }
    var contentWidth by remember { mutableStateOf(0f) }
    var contentHeight by remember { mutableStateOf(0f) }

    // Вычисляем масштаб чтобы формула поместилась целиком
    val autoScale = remember(containerWidth, containerHeight, contentWidth, contentHeight) {
        if (contentWidth > 0 && contentHeight > 0 && containerWidth > 0 && containerHeight > 0) {
            val scaleX = (containerWidth - 32f) / contentWidth // 32 = padding
            val scaleY = (containerHeight - 32f) / contentHeight
            minOf(scaleX, scaleY, 1f).coerceIn(0.3f, 1f)
        } else {
            1f
        }
    }

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
            .onGloballyPositioned { coordinates ->
                boundsRegistry.registerFormulaArea(coordinates.boundsInRoot())
            }
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
        // Контент с автомасштабированием
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { coordinates ->
                    containerWidth = coordinates.size.width.toFloat()
                    containerHeight = coordinates.size.height.toFloat()
                }
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
                // Применяем автомасштабирование
                Box(
                    modifier = Modifier
                        .onGloballyPositioned { coordinates ->
                            contentWidth = coordinates.size.width.toFloat()
                            contentHeight = coordinates.size.height.toFloat()
                        }
                        .graphicsLayer {
                            scaleX = autoScale
                            scaleY = autoScale
                        }
                ) {
                    FormulaRenderer(
                        elements = elements,
                        dragState = dragState,
                        hoverState = hoverState,
                        onDragStart = onDragStart,
                        onDragEnd = onDragEnd,
                        onDragMove = onDragMove,
                        onEllipsisClick = onEllipsisClick,
                        onVariableClick = onVariableClick,
                        onParenthesesClick = onParenthesesClick,
                        onOperatorClick = onOperatorClick,
                        variableValues = variableValues
                    )
                }
            }
        }

        // Индикатор масштаба (показываем если формула уменьшена)
        if (autoScale < 0.95f) {
            Text(
                text = "${(autoScale * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
            )
        }
    }
}
