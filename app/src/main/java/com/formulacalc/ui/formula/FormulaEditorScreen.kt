package com.formulacalc.ui.formula

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
                    onDragStart = { element, offset -> viewModel.onDragStart(element, offset) },
                    onDragEnd = { viewModel.onDragEnd() },
                    onDragMove = { viewModel.onDragMove(it) },
                    onEllipsisClick = { viewModel.onEllipsisClick(it) },
                    onVariableClick = { viewModel.onVariableClick(it) },
                    modifier = Modifier.weight(1f)
                )

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
    onVariableClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            )
            .background(MaterialTheme.colorScheme.surface)
            .horizontalScroll(scrollState)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (elements.isEmpty()) {
            Text(
                text = "Формула пуста",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
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
                onVariableClick = onVariableClick
            )
        }
    }
}
