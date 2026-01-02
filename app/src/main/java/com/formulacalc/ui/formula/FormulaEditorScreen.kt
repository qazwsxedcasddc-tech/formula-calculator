package com.formulacalc.ui.formula

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.formulacalc.viewmodel.FormulaEditorViewModel

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
                // Заголовок
                FormulaHeader(
                    onReset = { viewModel.reset() }
                )

                // Инструкции
                FormulaInstructions()

                // Область формулы
                FormulaArea(
                    elements = state.elements,
                    dragState = state.dragState,
                    hoverState = state.hoverState,
                    onDragStart = { element, offset -> viewModel.onDragStart(element, offset) },
                    onDragEnd = { viewModel.onDragEnd() },
                    onDragMove = { viewModel.onDragMove(it) },
                    onEllipsisClick = { viewModel.onEllipsisClick(it) },
                    onVariableClick = { viewModel.onVariableClick(it) }
                )
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
        }
    }
}

/**
 * Заголовок редактора
 */
@Composable
private fun FormulaHeader(
    onReset: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Редактор формул",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Кнопка сброса
        Box(
            modifier = Modifier
                .size(40.dp)
                .shadow(8.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFFFF6B6B), Color(0xFFEE5A5A))
                    )
                )
                .clickable { onReset() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "✕",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Инструкции по использованию
 */
@Composable
private fun FormulaInstructions() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(16.dp)
    ) {
        Text(
            text = "Удерживайте переменную для перетаскивания",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(16.dp)
                    .background(FormulaColors.dropIndicatorGreen, RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Зелёная линия — вставка рядом",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(16.dp)
                    .height(4.dp)
                    .background(FormulaColors.dropIndicatorPurple, RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Фиолетовая линия — создание дроби",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "···",
                color = FormulaColors.ellipsisText,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Нажмите — выбор оператора",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(FormulaColors.variableGradientStart)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Нажмите на переменную — редактирование степени",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Область с формулой
 */
@Composable
private fun FormulaArea(
    elements: List<com.formulacalc.model.FormulaElement>,
    dragState: DragState,
    hoverState: HoverState,
    onDragStart: (com.formulacalc.model.FormulaElement, androidx.compose.ui.geometry.Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragMove: (androidx.compose.ui.geometry.Offset) -> Unit,
    onEllipsisClick: (String) -> Unit,
    onVariableClick: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp)
            )
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                )
            )
            .padding(24.dp)
            .heightIn(min = 120.dp),
        contentAlignment = Alignment.Center
    ) {
        FormulaRenderer(
            elements = elements,
            dragState = dragState,
            hoverState = hoverState,
            onDragStart = onDragStart,
            onDragEnd = onDragEnd,
            onDragMove = onDragMove,
            onEllipsisClick = onEllipsisClick,
            onVariableClick = onVariableClick
        )
    }
}
