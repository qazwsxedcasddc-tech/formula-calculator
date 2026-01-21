package com.formulacalc.viewmodel

import androidx.lifecycle.ViewModel
import com.formulacalc.model.Formula
import com.formulacalc.model.FormulaToken
import com.formulacalc.model.PresetFormula
import com.formulacalc.parser.EvalResult
import com.formulacalc.parser.evaluateFormula
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
 * Состояние UI
 */
data class FormulaUiState(
    val formula: Formula = Formula(),
    val result: String? = null,
    val error: String? = null,
    val selectedTab: TabIndex = TabIndex.CALCULATOR,
    val isDragOver: Boolean = false,
    val isResultDisplayed: Boolean = false
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
        _uiState.value = _uiState.value.copy(
            formula = _uiState.value.formula.insertToken(token),
            result = null,
            error = null,
            isResultDisplayed = false
        )
    }

    /**
     * Вставить число (по цифрам)
     */
    fun insertDigit(digit: String) {
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
        val currentFormula = _uiState.value.formula
        val tokens = currentFormula.tokens
        val cursorPos = currentFormula.cursorPosition

        if (cursorPos > 0) {
            val prevToken = tokens[cursorPos - 1]

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
                return
            }
        }

        // Иначе удаляем весь токен
        _uiState.value = _uiState.value.copy(
            formula = _uiState.value.formula.deleteToken(),
            result = null,
            error = null
        )
    }

    /**
     * Очистить формулу
     */
    fun clear() {
        _uiState.value = _uiState.value.copy(
            formula = Formula(),
            result = null,
            error = null,
            isResultDisplayed = false
        )
    }

    /**
     * Вычислить формулу
     */
    fun evaluate() {
        val formula = _uiState.value.formula
        if (formula.isEmpty()) {
            _uiState.value = _uiState.value.copy(error = "Формула пуста")
            return
        }

        when (val result = evaluateFormula(formula.tokens, variables)) {
            is EvalResult.Success -> {
                _uiState.value = _uiState.value.copy(
                    result = result.formatted(),
                    error = null,
                    isResultDisplayed = true
                )
            }

            is EvalResult.Error -> {
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
        _uiState.value = _uiState.value.copy(
            formula = Formula.fromTokens(preset.tokens),
            result = null,
            error = null,
            isResultDisplayed = false
        )
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
        _uiState.value = _uiState.value.copy(selectedTab = tab)
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
