package com.formulacalc.viewmodel

import androidx.lifecycle.ViewModel
import com.formulacalc.model.Formula
import com.formulacalc.model.FormulaToken
import com.formulacalc.model.PresetFormula
import com.formulacalc.parser.EvalResult
import com.formulacalc.parser.evaluateFormula
import com.formulacalc.util.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Индекс вкладки
 * Два режима: Калькулятор (цифры + инженерные) и Формулы (греческие + шаблоны)
 */
enum class TabIndex(val index: Int, val title: String) {
    CALCULATOR(0, "Калькулятор"),
    FORMULAS(1, "Формулы")
}

/**
 * Вариант раскладки калькулятора
 */
enum class CalculatorLayout {
    CLASSIC,      // Вариант A: Классический научный калькулятор
    TWO_PANEL,    // Вариант B: Двухпанельный со свайпом
    DRAWER        // Вариант C: Выдвижные панели
}

/**
 * Состояние UI
 */
data class FormulaUiState(
    val formula: Formula = Formula(),
    val result: String? = null,
    val error: String? = null,
    val selectedTab: TabIndex = TabIndex.CALCULATOR,
    val isDragOver: Boolean = false,
    val isResultDisplayed: Boolean = false,
    val calculatorLayout: CalculatorLayout = CalculatorLayout.CLASSIC
)

/**
 * ViewModel для управления формулой.
 *
 * Отвечает за:
 * - Хранение текущей формулы
 * - Вставку/удаление токенов
 * - Вычисление формулы
 * - Управление состоянием UI (вкладки, drag & drop)
 */
class FormulaViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(FormulaUiState())
    val uiState: StateFlow<FormulaUiState> = _uiState.asStateFlow()

    /**
     * Карта переменных для вычислений
     */
    private val variables = mutableMapOf<String, Double>()

    // ===== Работа с токенами =====

    /**
     * Вставить токен в текущую позицию курсора
     */
    fun insertToken(token: FormulaToken) {
        AppLogger.userTap("токен", token.displayText)
        _uiState.value = _uiState.value.copy(
            formula = _uiState.value.formula.insertToken(token),
            result = null,
            error = null,
            isResultDisplayed = false
        )
        AppLogger.formulaChanged(_uiState.value.formula.toDisplayString())
    }

    /**
     * Вставить число (по цифрам)
     */
    fun insertDigit(digit: String) {
        AppLogger.userTap("цифра", digit)
        val currentFormula = _uiState.value.formula
        val tokens = currentFormula.tokens
        val cursorPos = currentFormula.cursorPosition

        // Проверяем, есть ли число перед курсором
        if (cursorPos > 0) {
            val prevToken = tokens[cursorPos - 1]
            if (prevToken is FormulaToken.Number) {
                // Добавляем цифру к существующему числу
                val newValue = prevToken.value + digit
                val newTokens = tokens.toMutableList().apply {
                    set(cursorPos - 1, FormulaToken.Number(newValue))
                }
                _uiState.value = _uiState.value.copy(
                    formula = Formula(newTokens, cursorPos),
                    result = null,
                    error = null,
                    isResultDisplayed = false
                )
                AppLogger.formulaChanged(_uiState.value.formula.toDisplayString())
                return
            }
        }

        // Иначе создаём новое число
        insertToken(FormulaToken.Number(digit))
    }

    /**
     * Вставить точку в число
     */
    fun insertDecimalPoint() {
        AppLogger.userTap("десятичная точка", ".")
        val currentFormula = _uiState.value.formula
        val tokens = currentFormula.tokens
        val cursorPos = currentFormula.cursorPosition

        if (cursorPos > 0) {
            val prevToken = tokens[cursorPos - 1]
            if (prevToken is FormulaToken.Number && !prevToken.value.contains(".")) {
                val newValue = prevToken.value + "."
                val newTokens = tokens.toMutableList().apply {
                    set(cursorPos - 1, FormulaToken.Number(newValue))
                }
                _uiState.value = _uiState.value.copy(
                    formula = Formula(newTokens, cursorPos),
                    result = null,
                    error = null,
                    isResultDisplayed = false
                )
                AppLogger.formulaChanged(_uiState.value.formula.toDisplayString())
                return
            }
        }

        // Если нет числа — начинаем с "0."
        insertToken(FormulaToken.Number("0."))
    }

    /**
     * Удалить токен перед курсором (backspace)
     */
    fun deleteToken() {
        AppLogger.userTap("backspace", "удаление")
        val currentFormula = _uiState.value.formula
        val tokens = currentFormula.tokens
        val cursorPos = currentFormula.cursorPosition

        if (cursorPos > 0) {
            val prevToken = tokens[cursorPos - 1]
            AppLogger.log("ACTION", "Удаление элемента: ${prevToken.displayText}")

            // Если это число с несколькими цифрами — удаляем последнюю цифру
            if (prevToken is FormulaToken.Number && prevToken.value.length > 1) {
                val newValue = prevToken.value.dropLast(1)
                val newTokens = tokens.toMutableList().apply {
                    set(cursorPos - 1, FormulaToken.Number(newValue))
                }
                _uiState.value = _uiState.value.copy(
                    formula = Formula(newTokens, cursorPos),
                    result = null,
                    error = null
                )
                AppLogger.formulaChanged(_uiState.value.formula.toDisplayString())
                return
            }
        }

        // Иначе удаляем весь токен
        _uiState.value = _uiState.value.copy(
            formula = _uiState.value.formula.deleteToken(),
            result = null,
            error = null
        )
        AppLogger.formulaChanged(_uiState.value.formula.toDisplayString())
    }

    /**
     * Очистить формулу
     */
    fun clear() {
        AppLogger.userReset()
        _uiState.value = _uiState.value.copy(
            formula = Formula(),
            result = null,
            error = null,
            isResultDisplayed = false
        )
        AppLogger.log("ACTION", "Формула очищена")
    }

    /**
     * Вычислить формулу
     */
    fun evaluate() {
        AppLogger.userTap("equals", "вычисление")
        val formula = _uiState.value.formula
        if (formula.isEmpty()) {
            AppLogger.calculationError("Формула пуста")
            _uiState.value = _uiState.value.copy(error = "Формула пуста")
            return
        }

        AppLogger.calculationStarted(formula.toDisplayString(), variables.toMap())

        when (val result = evaluateFormula(formula.tokens, variables)) {
            is EvalResult.Success -> {
                AppLogger.calculationResult(result.value, formula.toDisplayString())
                _uiState.value = _uiState.value.copy(
                    result = result.formatted(),
                    error = null,
                    isResultDisplayed = true
                )
            }

            is EvalResult.Error -> {
                AppLogger.calculationError(result.message, formula.toDisplayString())
                _uiState.value = _uiState.value.copy(
                    result = null,
                    error = result.message
                )
            }
        }
    }

    /**
     * Установить готовую формулу (из пресетов)
     */
    fun setPresetFormula(preset: PresetFormula) {
        AppLogger.userDropPreset(preset.name)
        _uiState.value = _uiState.value.copy(
            formula = Formula.fromTokens(preset.tokens),
            result = null,
            error = null,
            isResultDisplayed = false
        )
        AppLogger.formulaChanged(_uiState.value.formula.toDisplayString())
    }

    /**
     * Установить формулу из списка токенов
     */
    fun setFormula(tokens: List<FormulaToken>) {
        _uiState.value = _uiState.value.copy(
            formula = Formula.fromTokens(tokens),
            result = null,
            error = null,
            isResultDisplayed = false
        )
    }

    // ===== Навигация по курсору =====

    /**
     * Переместить курсор влево
     */
    fun moveCursorLeft() {
        _uiState.value = _uiState.value.copy(
            formula = _uiState.value.formula.moveCursorLeft()
        )
    }

    /**
     * Переместить курсор вправо
     */
    fun moveCursorRight() {
        _uiState.value = _uiState.value.copy(
            formula = _uiState.value.formula.moveCursorRight()
        )
    }

    /**
     * Установить позицию курсора
     */
    fun setCursorPosition(position: Int) {
        _uiState.value = _uiState.value.copy(
            formula = _uiState.value.formula.setCursorPosition(position)
        )
    }

    // ===== Управление вкладками =====

    /**
     * Выбрать вкладку
     */
    fun selectTab(tab: TabIndex) {
        AppLogger.tabSelected(tab.title)
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    /**
     * Переключить раскладку калькулятора
     */
    fun setCalculatorLayout(layout: CalculatorLayout) {
        AppLogger.log("UI", "Смена раскладки на: ${layout.name}")
        _uiState.value = _uiState.value.copy(calculatorLayout = layout)
    }

    /**
     * Переключить на следующую раскладку (для тестирования)
     */
    fun cycleCalculatorLayout() {
        val current = _uiState.value.calculatorLayout
        val next = when (current) {
            CalculatorLayout.CLASSIC -> CalculatorLayout.TWO_PANEL
            CalculatorLayout.TWO_PANEL -> CalculatorLayout.DRAWER
            CalculatorLayout.DRAWER -> CalculatorLayout.CLASSIC
        }
        AppLogger.log("UI", "Переключение раскладки: ${current.name} → ${next.name}")
        _uiState.value = _uiState.value.copy(calculatorLayout = next)
    }

    // ===== Drag & Drop =====

    /**
     * Установить состояние drag over
     */
    fun setDragOver(isDragOver: Boolean) {
        _uiState.value = _uiState.value.copy(isDragOver = isDragOver)
    }

    /**
     * Обработать drop токена
     */
    fun onTokenDropped(token: FormulaToken, dropPosition: Int? = null) {
        AppLogger.userDragEnd(token.displayText, "формула", "позиция ${dropPosition ?: "курсор"}")
        val formula = _uiState.value.formula

        val newFormula = if (dropPosition != null) {
            // Вставляем в указанную позицию
            val tokens = formula.tokens.toMutableList().apply {
                add(dropPosition, token)
            }
            Formula(tokens, dropPosition + 1)
        } else {
            // Вставляем в позицию курсора
            formula.insertToken(token)
        }

        _uiState.value = _uiState.value.copy(
            formula = newFormula,
            isDragOver = false,
            result = null,
            error = null
        )
        AppLogger.formulaChanged(_uiState.value.formula.toDisplayString())
    }

    /**
     * Обработать drop готовой формулы (заменяет текущую)
     */
    fun onPresetDropped(preset: PresetFormula) {
        setPresetFormula(preset)
        _uiState.value = _uiState.value.copy(isDragOver = false)
    }

    // ===== Переменные =====

    /**
     * Установить значение переменной
     */
    fun setVariable(name: String, value: Double) {
        variables[name] = value
    }

    /**
     * Получить значение переменной
     */
    fun getVariable(name: String): Double? = variables[name]

    /**
     * Очистить все переменные
     */
    fun clearVariables() {
        variables.clear()
    }
}
