package com.formulacalc.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.formulacalc.model.FormulaToken
import com.formulacalc.model.OperatorType
import com.formulacalc.ui.formula.FormulaEditorScreen
import com.formulacalc.viewmodel.FormulaEditorViewModel
import com.formulacalc.viewmodel.CalculatorLayout
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
                // ===== Кнопка переключения раскладки =====
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (uiState.calculatorLayout) {
                            CalculatorLayout.CLASSIC -> "A: Классический"
                            CalculatorLayout.TWO_PANEL -> "B: Двухпанельный"
                            CalculatorLayout.DRAWER -> "C: Выдвижной"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(
                        onClick = { viewModel.cycleCalculatorLayout() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Filled.Menu,
                            contentDescription = "Сменить раскладку",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // ===== Зона A: Редактор формул =====
                FormulaEditorScreen(
                    viewModel = editorViewModel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.45f)
                )

                // ===== Зона B: Вкладки режимов (только для CLASSIC) =====
                if (uiState.calculatorLayout == CalculatorLayout.CLASSIC) {
                    TabsRow(
                        selectedTab = uiState.selectedTab,
                        onTabSelected = { viewModel.selectTab(it) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // ===== Зона C: Панель ввода =====
                InputPanel(
                    selectedTab = uiState.selectedTab,
                    calculatorLayout = uiState.calculatorLayout,
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
                            is FormulaToken.Number -> {
                                // Числа добавляем как переменные
                                editorViewModel.addVariable(token.value)
                            }
                            is FormulaToken.Function -> {
                                // Функции добавляем как переменные
                                editorViewModel.addVariable(token.displayText)
                            }
                        }
                    },
                    onDigitClick = { editorViewModel.addNumber(it) },
                    onDecimalClick = { editorViewModel.addDecimalPoint() },
                    onClearClick = {
                        viewModel.clear()
                        editorViewModel.clearFormula()
                    },
                    onBackspaceClick = { editorViewModel.deleteLastElement() },
                    onEqualsClick = { editorViewModel.calculateResult() },
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
