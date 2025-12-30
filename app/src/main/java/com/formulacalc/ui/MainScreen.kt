package com.formulacalc.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.formulacalc.model.FormulaToken
import com.formulacalc.viewmodel.FormulaViewModel
import com.formulacalc.viewmodel.TabIndex

/**
 * Главный экран приложения.
 *
 * Структура:
 * - Зона A: FormulaDisplay (окно формулы) — ~30% экрана
 * - Зона B: TabsRow (вкладки режимов)
 * - Зона C: InputPanel (панель ввода) — остальное
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: FormulaViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Оборачиваем в DragDropProvider для поддержки drag & drop
    DragDropProvider {
        Scaffold(
            modifier = Modifier.fillMaxSize()
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // ===== Зона A: Окно формулы =====
                FormulaDisplay(
                    formula = uiState.formula,
                    result = uiState.result,
                    error = uiState.error,
                    isDragOver = uiState.isDragOver,
                    isResultDisplayed = uiState.isResultDisplayed,
                    onCursorPositionChanged = { viewModel.setCursorPosition(it) },
                    onDragOver = { viewModel.setDragOver(it) },
                    onTokenDropped = { token, position -> viewModel.onTokenDropped(token, position) },
                    onDrop = { dragData ->
                        when (dragData) {
                            is DragData.Token -> viewModel.onTokenDropped(dragData.token, null)
                            is DragData.Preset -> viewModel.onPresetDropped(dragData.preset)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.3f)
                        .padding(16.dp)
                )

                // ===== Зона B: Вкладки режимов =====
                TabsRow(
                    selectedTab = uiState.selectedTab,
                    onTabSelected = { viewModel.selectTab(it) },
                    modifier = Modifier.fillMaxWidth()
                )

                // ===== Зона C: Панель ввода =====
                InputPanel(
                    selectedTab = uiState.selectedTab,
                    onTokenClick = { viewModel.insertToken(it) },
                    onDigitClick = { viewModel.insertDigit(it) },
                    onDecimalClick = { viewModel.insertDecimalPoint() },
                    onClearClick = { viewModel.clear() },
                    onBackspaceClick = { viewModel.deleteToken() },
                    onEqualsClick = { viewModel.evaluate() },
                    onPresetClick = { viewModel.setPresetFormula(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.7f)
                )
            }
        }
    }
}
