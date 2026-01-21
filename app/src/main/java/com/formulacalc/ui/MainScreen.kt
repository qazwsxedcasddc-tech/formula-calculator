package com.formulacalc.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.formulacalc.model.FormulaToken
import com.formulacalc.model.OperatorType
import com.formulacalc.ui.formula.FormulaEditorScreen
import com.formulacalc.viewmodel.FormulaEditorViewModel
import com.formulacalc.viewmodel.FormulaViewModel
import com.formulacalc.viewmodel.TabIndex

/**
 * Главный экран приложения.
 *
 * Структура:
 * - Зона A: FormulaEditorScreen (редактор формул с drag & drop) — ~40% экрана
 * - Зона B: TabsRow (вкладки режимов)
 * - Зона C: InputPanel (панель ввода) — остальное
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: FormulaViewModel = viewModel(),
    editorViewModel: FormulaEditorViewModel = viewModel()
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
                // ===== Зона A: Редактор формул =====
                FormulaEditorScreen(
                    viewModel = editorViewModel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.45f)
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
                    onTokenClick = { token ->
                        // Добавляем элемент в редактор формул
                        when (token) {
                            is FormulaToken.Variable -> editorViewModel.addVariable(token.name)
                            is FormulaToken.Subscript -> editorViewModel.addVariable(token.base, token.displayText)
                            is FormulaToken.Superscript -> editorViewModel.addVariable(token.base, token.displayText)
                            is FormulaToken.Operator -> {
                                val opType = when (token.symbol) {
                                    "+" -> OperatorType.PLUS
                                    "−" -> OperatorType.MINUS
                                    "×" -> OperatorType.MULTIPLY
                                    "÷" -> OperatorType.DIVIDE
                                    else -> null
                                }
                                opType?.let { editorViewModel.addOperator(it) }
                            }
                            is FormulaToken.Parenthesis -> {
                                val opType = if (token.isOpen) OperatorType.OPEN_PAREN else OperatorType.CLOSE_PAREN
                                editorViewModel.addOperator(opType)
                            }
                            else -> {
                                // Для остальных токенов используем старую логику
                                viewModel.insertToken(token)
                            }
                        }
                    },
                    onDigitClick = { viewModel.insertDigit(it) },
                    onDecimalClick = { viewModel.insertDecimalPoint() },
                    onClearClick = {
                        viewModel.clear()
                        editorViewModel.clearFormula()
                    },
                    onBackspaceClick = { viewModel.deleteToken() },
                    onEqualsClick = { viewModel.evaluate() },
                    onPresetClick = { viewModel.setPresetFormula(it) },
                    onPresetDoubleTap = { preset ->
                        // Двойной тап — загрузить формулу (заменить текущую)
                        editorViewModel.loadPresetFormula(preset)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.55f)
                )
            }
        }
    }
}
