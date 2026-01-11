package com.formulacalc.viewmodel

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import com.formulacalc.model.*
import com.formulacalc.model.toLogString
import com.formulacalc.model.findById
import com.formulacalc.ui.formula.DragState
import com.formulacalc.ui.formula.ElementBoundsRegistry
import com.formulacalc.ui.formula.HoverState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import android.util.Log

/**
 * Состояние редактора формул
 */
data class FormulaEditorState(
    val elements: List<FormulaElement> = getInitialGravityFormula(),
    val dragState: DragState = DragState(),
    val hoverState: HoverState = HoverState(),
    val showOperatorMenu: Boolean = false,
    val operatorMenuTargetId: String? = null,
    val showExponentKeyboard: Boolean = false,
    val exponentKeyboardTargetId: String? = null,
    val currentExponent: Exponent? = null,
    // Для ввода значений переменных
    val showVariableInput: Boolean = false,
    val variableInputTargetId: String? = null,
    val variableInputName: String = "",
    // Значения переменных: ключ = имя переменной (displayValue), значение = число
    val variableValues: Map<String, Double> = emptyMap(),
    // Результат вычисления
    val calculationResult: Double? = null,
    val calculationError: String? = null
)

/**
 * ViewModel для редактора формул с поддержкой drag & drop
 */
class FormulaEditorViewModel : ViewModel() {

    private val _state = MutableStateFlow(FormulaEditorState())
    val state: StateFlow<FormulaEditorState> = _state.asStateFlow()

    // Registry для отслеживания границ элементов
    val boundsRegistry = ElementBoundsRegistry()

    // ===== Drag & Drop =====

    /**
     * Начало перетаскивания элемента
     */
    fun onDragStart(element: FormulaElement, fingerPosition: Offset) {
        Log.d("DragDrop", "═══════════════════════════════════════")
        Log.d("DragDrop", "🟢 DRAG START: ${element.toLogString()}")
        Log.d("DragDrop", "   Position: $fingerPosition")
        Log.d("DragDrop", "   Current formula: ${_state.value.elements.toLogString()}")
        boundsRegistry.logAllBounds("   ")
        _state.update {
            it.copy(
                dragState = DragState(
                    isDragging = true,
                    draggedElement = element,
                    fingerPosition = fingerPosition
                )
            )
        }
    }

    /**
     * Перемещение пальца — получаем абсолютную позицию
     */
    fun onDragMove(fingerPosition: Offset) {
        val currentState = _state.value
        if (!currentState.dragState.isDragging) return

        // Найти элемент под курсором
        val draggedId = currentState.dragState.draggedElement?.id
        val dropTarget = boundsRegistry.findDropTarget(fingerPosition, draggedId)

        // Логируем только при изменении target
        val prevTarget = currentState.hoverState.targetId
        val prevSide = currentState.hoverState.side
        if (dropTarget?.first != prevTarget || dropTarget?.second != prevSide) {
            if (dropTarget != null) {
                val targetElement = currentState.elements.findById(dropTarget.first)
                Log.d("DragDrop", "🎯 HOVER: target=${targetElement?.toLogString() ?: "?"}, side=${dropTarget.second}")
            } else if (prevTarget != null) {
                Log.d("DragDrop", "🎯 HOVER: cleared (no target)")
            }
        }

        _state.update {
            it.copy(
                dragState = it.dragState.copy(fingerPosition = fingerPosition),
                hoverState = if (dropTarget != null) {
                    HoverState(targetId = dropTarget.first, side = dropTarget.second)
                } else {
                    HoverState()
                }
            )
        }
    }

    /**
     * Окончание перетаскивания
     */
    fun onDragEnd() {
        val currentState = _state.value
        val draggedElement = currentState.dragState.draggedElement
        val targetId = currentState.hoverState.targetId
        val side = currentState.hoverState.side

        Log.d("DragDrop", "═══════════════════════════════════════")
        Log.d("DragDrop", "🔴 DRAG END")
        Log.d("DragDrop", "   Dragged: ${draggedElement?.toLogString() ?: "null"}")
        Log.d("DragDrop", "   Target ID: $targetId")
        Log.d("DragDrop", "   Side: $side")

        if (draggedElement != null && targetId != null && side != null && draggedElement.id != targetId) {
            val targetElement = currentState.elements.findById(targetId)
            Log.d("DragDrop", "   Target Element: ${targetElement?.toLogString() ?: "NOT FOUND"}")
            Log.d("DragDrop", "   BEFORE: ${currentState.elements.toLogString()}")

            // Удаляем элемент из старой позиции
            val withoutDragged = currentState.elements.removeById(draggedElement.id)
            Log.d("DragDrop", "   After remove: ${withoutDragged.toLogString()}")

            // Вставляем в новую позицию (клонируем чтобы получить новый ID)
            val clonedElement = draggedElement.clone()
            val newElements = withoutDragged.insertAt(clonedElement, targetId, side)
            Log.d("DragDrop", "   AFTER: ${newElements.toLogString()}")
            Log.d("DragDrop", "═══════════════════════════════════════")

            _state.update {
                it.copy(
                    elements = newElements,
                    dragState = DragState(),
                    hoverState = HoverState()
                )
            }
        } else {
            Log.d("DragDrop", "   ❌ Drop cancelled (no valid target)")
            Log.d("DragDrop", "═══════════════════════════════════════")
            // Просто сбрасываем drag state
            _state.update {
                it.copy(
                    dragState = DragState(),
                    hoverState = HoverState()
                )
            }
        }
    }

    /**
     * Сброс формулы к начальному состоянию
     */
    fun reset() {
        boundsRegistry.clear()
        _state.update {
            FormulaEditorState()
        }
    }

    // ===== Меню оператора =====

    /**
     * Клик на ellipsis — показать меню выбора оператора
     */
    fun onEllipsisClick(id: String) {
        _state.update {
            it.copy(
                showOperatorMenu = true,
                operatorMenuTargetId = id
            )
        }
    }

    /**
     * Выбор оператора из меню
     */
    fun selectOperator(type: OperatorType) {
        val targetId = _state.value.operatorMenuTargetId ?: return

        _state.update {
            it.copy(
                elements = it.elements.replaceEllipsis(targetId, type),
                showOperatorMenu = false,
                operatorMenuTargetId = null
            )
        }
    }

    /**
     * Закрытие меню оператора
     */
    fun dismissOperatorMenu() {
        _state.update {
            it.copy(
                showOperatorMenu = false,
                operatorMenuTargetId = null
            )
        }
    }

    // ===== Клавиатура экспоненты =====

    /**
     * Клик на переменную — показать клавиатуру степени
     */
    fun onVariableClick(id: String) {
        val element = _state.value.elements.findById(id)
        val currentExponent = (element as? FormulaElement.Variable)?.exponent

        _state.update {
            it.copy(
                showExponentKeyboard = true,
                exponentKeyboardTargetId = id,
                currentExponent = currentExponent
            )
        }
    }

    /**
     * Сохранение экспоненты
     */
    fun saveExponent(exponent: Exponent?) {
        val targetId = _state.value.exponentKeyboardTargetId ?: return

        _state.update {
            it.copy(
                elements = it.elements.updateExponent(targetId, exponent),
                showExponentKeyboard = false,
                exponentKeyboardTargetId = null,
                currentExponent = null
            )
        }
    }

    /**
     * Закрытие клавиатуры экспоненты
     */
    fun dismissExponentKeyboard() {
        _state.update {
            it.copy(
                showExponentKeyboard = false,
                exponentKeyboardTargetId = null,
                currentExponent = null
            )
        }
    }

    // ===== Добавление элементов =====

    /**
     * Добавить переменную в конец формулы
     */
    fun addVariable(value: String, displayValue: String? = null) {
        _state.update {
            val newElement = createVariable(value, displayValue)
            it.copy(
                elements = (it.elements + newElement).normalize()
            )
        }
    }

    /**
     * Добавить оператор в конец формулы
     */
    fun addOperator(type: OperatorType) {
        _state.update {
            val newElement = createOperator(type)
            it.copy(
                elements = (it.elements + newElement).normalize()
            )
        }
    }

    /**
     * Установить формулу из списка элементов
     */
    fun setFormula(elements: List<FormulaElement>) {
        boundsRegistry.clear()
        _state.update {
            it.copy(elements = elements)
        }
    }

    // ===== Drop preset formula =====

    /**
     * Обработка drop формулы из нижней панели.
     * Конвертирует PresetFormula в элементы и добавляет к текущей формуле.
     *
     * - Берёт только правую часть формулы (после =)
     * - Автоматически добавляет ellipsis между существующими элементами и новыми
     * - Деление отображается как дробь
     */
    fun dropPreset(preset: PresetFormula) {
        Log.d("FormulaEditor", "dropPreset called: ${preset.name}")

        // Конвертируем preset в элементы (только правая часть)
        val newElements = preset.toFormulaElements()
        Log.d("FormulaEditor", "Converted to ${newElements.size} elements")

        _state.update { currentState ->
            // Добавляем к существующей формуле с автоматическим ellipsis
            val updatedElements = currentState.elements.appendElements(newElements)
            Log.d("FormulaEditor", "Total elements now: ${updatedElements.size}")

            currentState.copy(elements = updatedElements)
        }
    }

    /**
     * Очистить формулу и установить пустую с "F ="
     */
    fun clearAndSetEmpty() {
        boundsRegistry.clear()
        _state.update {
            it.copy(
                elements = listOf(
                    createVariable("F"),
                    createEquals()
                )
            )
        }
    }

    // ===== Ввод значений переменных =====

    /**
     * Клик на переменную для ввода значения (короткий тап)
     */
    fun onVariableClickForValue(id: String) {
        val element = _state.value.elements.findById(id)
        if (element is FormulaElement.Variable) {
            _state.update {
                it.copy(
                    showVariableInput = true,
                    variableInputTargetId = id,
                    variableInputName = element.displayValue
                )
            }
        }
    }

    /**
     * Сохранить значение переменной
     */
    fun setVariableValue(variableName: String, value: Double?) {
        _state.update { state ->
            val newValues = if (value != null) {
                state.variableValues + (variableName to value)
            } else {
                state.variableValues - variableName
            }
            state.copy(
                variableValues = newValues,
                showVariableInput = false,
                variableInputTargetId = null,
                variableInputName = ""
            )
        }
        // Пересчитываем результат
        calculateResult()
    }

    /**
     * Закрыть диалог ввода переменной
     */
    fun dismissVariableInput() {
        _state.update {
            it.copy(
                showVariableInput = false,
                variableInputTargetId = null,
                variableInputName = ""
            )
        }
    }

    /**
     * Получить значение переменной
     */
    fun getVariableValue(variableName: String): Double? {
        return _state.value.variableValues[variableName]
    }

    /**
     * Вычислить результат формулы
     */
    fun calculateResult() {
        val state = _state.value

        // Собираем все переменные из формулы
        val allVariables = collectVariables(state.elements)

        // Проверяем, все ли переменные заданы
        val missingVariables = allVariables.filter {
            !state.variableValues.containsKey(it) && !isConstant(it)
        }

        if (missingVariables.isNotEmpty()) {
            _state.update {
                it.copy(
                    calculationResult = null,
                    calculationError = null // Не ошибка, просто не все переменные заданы
                )
            }
            return
        }

        try {
            // Конвертируем формулу в строку и вычисляем
            val formulaString = elementsToString(state.elements, state.variableValues)
            Log.d("Calculator", "Formula string: $formulaString")

            // Простое вычисление (можно заменить на полноценный парсер)
            val result = evaluateSimple(formulaString)

            _state.update {
                it.copy(
                    calculationResult = result,
                    calculationError = null
                )
            }
        } catch (e: Exception) {
            Log.e("Calculator", "Calculation error", e)
            _state.update {
                it.copy(
                    calculationResult = null,
                    calculationError = e.message
                )
            }
        }
    }

    /**
     * Собрать все имена переменных из формулы
     */
    private fun collectVariables(elements: List<FormulaElement>): Set<String> {
        val result = mutableSetOf<String>()
        for (element in elements) {
            when (element) {
                is FormulaElement.Variable -> result.add(element.displayValue)
                is FormulaElement.Fraction -> {
                    result.addAll(collectVariables(element.numerator))
                    result.addAll(collectVariables(element.denominator))
                }
                else -> {}
            }
        }
        return result
    }

    /**
     * Проверить, является ли имя константой
     */
    private fun isConstant(name: String): Boolean {
        return name in listOf("π", "e", "c", "G", "φ")
    }

    /**
     * Конвертировать элементы в строку для вычисления
     */
    private fun elementsToString(
        elements: List<FormulaElement>,
        values: Map<String, Double>
    ): String {
        val sb = StringBuilder()
        for (element in elements) {
            when (element) {
                is FormulaElement.Variable -> {
                    val value = values[element.displayValue] ?: getConstantValue(element.displayValue) ?: 1.0
                    sb.append(value)
                    element.exponent?.let { exp ->
                        when (exp) {
                            is Exponent.Simple -> sb.append("^${exp.value}")
                            is Exponent.Fraction -> sb.append("^(${exp.numerator}/${exp.denominator})")
                        }
                    }
                }
                is FormulaElement.Operator -> {
                    sb.append(when (element.type) {
                        OperatorType.PLUS -> "+"
                        OperatorType.MINUS -> "-"
                        OperatorType.MULTIPLY -> "*"
                        OperatorType.DIVIDE -> "/"
                        OperatorType.OPEN_PAREN -> "("
                        OperatorType.CLOSE_PAREN -> ")"
                    })
                }
                is FormulaElement.Ellipsis -> sb.append("*") // Placeholder → умножение
                is FormulaElement.Fraction -> {
                    sb.append("(")
                    sb.append(elementsToString(element.numerator, values))
                    sb.append(")/(")
                    sb.append(elementsToString(element.denominator, values))
                    sb.append(")")
                }
                is FormulaElement.Equals -> {} // Пропускаем
            }
        }
        return sb.toString()
    }

    /**
     * Получить значение константы
     */
    private fun getConstantValue(name: String): Double? {
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
     * Простое вычисление выражения
     */
    private fun evaluateSimple(expression: String): Double {
        // Используем JavaScript-подобный eval через Kotlin
        // Для продакшена лучше использовать полноценный парсер
        return evaluateExpression(expression)
    }

    /**
     * Рекурсивное вычисление выражения
     */
    private fun evaluateExpression(expr: String): Double {
        var expression = expr.trim()

        // Обработка скобок
        while (expression.contains("(")) {
            val start = expression.lastIndexOf("(")
            val end = expression.indexOf(")", start)
            if (end == -1) throw IllegalArgumentException("Mismatched parentheses")

            val inner = expression.substring(start + 1, end)
            val result = evaluateExpression(inner)
            expression = expression.substring(0, start) + result + expression.substring(end + 1)
        }

        // Обработка степени
        if (expression.contains("^")) {
            val parts = expression.split("^", limit = 2)
            val base = evaluateExpression(parts[0])
            val exp = evaluateExpression(parts[1])
            return Math.pow(base, exp)
        }

        // Обработка сложения/вычитания (низший приоритет)
        val addSubIndex = findLastOperator(expression, listOf('+', '-'))
        if (addSubIndex > 0) {
            val left = expression.substring(0, addSubIndex)
            val op = expression[addSubIndex]
            val right = expression.substring(addSubIndex + 1)
            return if (op == '+') {
                evaluateExpression(left) + evaluateExpression(right)
            } else {
                evaluateExpression(left) - evaluateExpression(right)
            }
        }

        // Обработка умножения/деления
        val mulDivIndex = findLastOperator(expression, listOf('*', '/'))
        if (mulDivIndex > 0) {
            val left = expression.substring(0, mulDivIndex)
            val op = expression[mulDivIndex]
            val right = expression.substring(mulDivIndex + 1)
            return if (op == '*') {
                evaluateExpression(left) * evaluateExpression(right)
            } else {
                evaluateExpression(left) / evaluateExpression(right)
            }
        }

        // Число
        return expression.toDoubleOrNull() ?: throw IllegalArgumentException("Invalid number: $expression")
    }

    /**
     * Найти последний оператор вне скобок
     */
    private fun findLastOperator(expr: String, operators: List<Char>): Int {
        var depth = 0
        for (i in expr.length - 1 downTo 0) {
            when (expr[i]) {
                ')' -> depth++
                '(' -> depth--
                in operators -> if (depth == 0 && i > 0) return i
            }
        }
        return -1
    }
}