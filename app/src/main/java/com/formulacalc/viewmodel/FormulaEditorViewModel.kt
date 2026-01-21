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
import com.formulacalc.util.AppLogger
import com.formulacalc.util.CalculationEntry
import com.formulacalc.util.CalculationHistory
import com.formulacalc.util.UndoRedoManager

/**
 * Состояние редактора формул
 */
data class FormulaEditorState(
    val elements: List<FormulaElement> = getInitialGravityFormula(),
    val dragState: DragState = DragState(),
    val hoverState: HoverState = HoverState(),
    val showOperatorMenu: Boolean = false,
    val operatorMenuTargetId: String? = null,
    val isOperatorReplaceMode: Boolean = false, // true если заменяем существующий оператор
    val showExponentKeyboard: Boolean = false,
    val exponentKeyboardTargetId: String? = null,
    val currentExponent: Exponent? = null,
    // Для ввода значений переменных
    val showVariableInput: Boolean = false,
    val variableInputTargetId: String? = null,
    val variableInputName: String = "",
    // Значения переменных: ключ = ID переменной (уникальный), значение = число
    val variableValues: Map<String, Double> = emptyMap(),
    // Результат вычисления
    val calculationResult: Double? = null,
    val calculationError: String? = null,
    // Undo/Redo состояние
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    // Snackbar для отмены удаления
    val showDeleteSnackbar: Boolean = false,
    val deletedElementName: String = "",
    // История вычислений
    val calculationHistory: List<CalculationEntry> = emptyList(),
    val showHistoryPanel: Boolean = false,
    // Диалог для скобок
    val showParenthesesDialog: Boolean = false,
    val parenthesesDialogTargetId: String? = null
)

/**
 * ViewModel для редактора формул с поддержкой drag & drop
 */
class FormulaEditorViewModel : ViewModel() {

    private val _state = MutableStateFlow(FormulaEditorState())
    val state: StateFlow<FormulaEditorState> = _state.asStateFlow()

    // Registry для отслеживания границ элементов
    val boundsRegistry = ElementBoundsRegistry()

    // Менеджер Undo/Redo
    private val undoRedoManager = UndoRedoManager()

    // История вычислений
    private val calculationHistory = CalculationHistory()

    // ===== Undo/Redo =====

    /**
     * Сохранить состояние перед изменением
     */
    private fun saveStateForUndo(actionName: String) {
        val currentState = _state.value
        undoRedoManager.saveState(
            elements = currentState.elements,
            variableValues = currentState.variableValues,
            actionName = actionName
        )
        updateUndoRedoState()
    }

    /**
     * Обновить состояние кнопок undo/redo
     */
    private fun updateUndoRedoState() {
        _state.update {
            it.copy(
                canUndo = undoRedoManager.canUndo(),
                canRedo = undoRedoManager.canRedo()
            )
        }
    }

    /**
     * Отменить последнее действие
     */
    fun undo() {
        val currentState = _state.value
        val snapshot = undoRedoManager.undo(
            currentElements = currentState.elements,
            currentVariableValues = currentState.variableValues
        )

        if (snapshot != null) {
            AppLogger.undoAction(snapshot.actionName)
            _state.update {
                it.copy(
                    elements = snapshot.elements,
                    variableValues = snapshot.variableValues
                )
            }
            updateUndoRedoState()
            calculateResult()
        }
    }

    /**
     * Повторить отменённое действие
     */
    fun redo() {
        val currentState = _state.value
        val snapshot = undoRedoManager.redo(
            currentElements = currentState.elements,
            currentVariableValues = currentState.variableValues
        )

        if (snapshot != null) {
            AppLogger.redoAction()
            _state.update {
                it.copy(
                    elements = snapshot.elements,
                    variableValues = snapshot.variableValues
                )
            }
            updateUndoRedoState()
            calculateResult()
        }
    }

    /**
     * Скрыть snackbar удаления
     */
    fun dismissDeleteSnackbar() {
        _state.update { it.copy(showDeleteSnackbar = false) }
    }

    /**
     * Переключить панель истории
     */
    fun toggleHistoryPanel() {
        _state.update { it.copy(showHistoryPanel = !it.showHistoryPanel) }
    }

    /**
     * Очистить историю
     */
    fun clearHistory() {
        calculationHistory.clear()
        _state.update { it.copy(calculationHistory = emptyList()) }
    }

    /**
     * Обновить список истории в state
     */
    private fun updateHistoryState() {
        _state.update { it.copy(calculationHistory = calculationHistory.getAll()) }
    }

    // ===== Drag & Drop =====

    /**
     * Начало перетаскивания элемента
     */
    fun onDragStart(element: FormulaElement, fingerPosition: Offset) {
        Log.d("DragDrop", "═══════════════════════════════════════")
        Log.d("DragDrop", "🟢 DRAG START: ${element.toLogString()}")
        Log.d("DragDrop", "   Position: $fingerPosition")
        Log.d("DragDrop", "   Current formula: ${_state.value.elements.toLogString()}")
        AppLogger.userDragStart(element.toLogString(), "формула")
        boundsRegistry.logAllBounds("   ")
        _state.update {
            it.copy(
                dragState = DragState(
                    isDragging = true,
                    draggedElement = element,
                    fingerPosition = fingerPosition,
                    startPosition = fingerPosition // Запоминаем начальную позицию
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
        val fingerPosition = currentState.dragState.fingerPosition
        val startPosition = currentState.dragState.startPosition

        Log.d("DragDrop", "═══════════════════════════════════════")
        Log.d("DragDrop", "🔴 DRAG END")
        Log.d("DragDrop", "   Dragged: ${draggedElement?.toLogString() ?: "null"}")
        Log.d("DragDrop", "   Target ID: $targetId")
        Log.d("DragDrop", "   Side: $side")
        Log.d("DragDrop", "   Finger pos: $fingerPosition, Start pos: $startPosition")

        if (draggedElement != null && targetId != null && side != null && draggedElement.id != targetId) {
            // Успешный drop на цель — сохраняем для undo
            saveStateForUndo("Перемещение ${draggedElement.toLogString()}")

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

            AppLogger.userDragEnd(draggedElement.toLogString(), targetElement?.toLogString(), side.name)
            AppLogger.formulaChanged(newElements.toLogString())

            _state.update {
                it.copy(
                    elements = newElements,
                    dragState = DragState(),
                    hoverState = HoverState()
                )
            }
        } else if (draggedElement != null) {
            // Нет valid target — проверяем, куда отпустили
            val isInsideFormulaArea = boundsRegistry.isInsideFormulaArea(fingerPosition, margin = 100f)

            Log.d("DragDrop", "   ❌ No valid target")
            Log.d("DragDrop", "   Inside formula area: $isInsideFormulaArea")
            AppLogger.debugDropPosition(fingerPosition.x.toInt(), fingerPosition.y.toInt(), isInsideFormulaArea)

            if (isInsideFormulaArea) {
                // Отпустили внутри области формулы — возвращаем на место (ничего не делаем)
                Log.d("DragDrop", "   → Returning to original position")
                Log.d("DragDrop", "═══════════════════════════════════════")
                AppLogger.userDragEnd(draggedElement.toLogString(), null, "RETURN_TO_PLACE")

                _state.update {
                    it.copy(
                        dragState = DragState(),
                        hoverState = HoverState()
                    )
                }
            } else {
                // Отпустили далеко за пределами — удаляем элемент
                // Сохраняем для undo
                saveStateForUndo("Удаление ${draggedElement.toLogString()}")

                Log.d("DragDrop", "   → DELETING element (dropped outside)")
                Log.d("DragDrop", "   BEFORE: ${currentState.elements.toLogString()}")

                val newElements = currentState.elements.removeById(draggedElement.id)
                Log.d("DragDrop", "   AFTER: ${newElements.toLogString()}")
                Log.d("DragDrop", "═══════════════════════════════════════")

                AppLogger.userDragEnd(draggedElement.toLogString(), null, "DELETED")
                AppLogger.formulaChanged(newElements.toLogString())

                // Получаем имя для snackbar
                val elementName = when (draggedElement) {
                    is FormulaElement.Variable -> draggedElement.displayValue
                    is FormulaElement.Fraction -> "дробь"
                    is FormulaElement.Parentheses -> "скобки"
                    else -> "элемент"
                }

                _state.update {
                    it.copy(
                        elements = newElements,
                        dragState = DragState(),
                        hoverState = HoverState(),
                        showDeleteSnackbar = true,
                        deletedElementName = elementName
                    )
                }
            }
        } else {
            Log.d("DragDrop", "   ❌ No dragged element")
            Log.d("DragDrop", "═══════════════════════════════════════")
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
        saveStateForUndo("Сброс формулы")
        AppLogger.userReset()
        boundsRegistry.clear()
        undoRedoManager.clear()
        _state.update {
            FormulaEditorState()
        }
    }

    /**
     * Очистить формулу (кнопка C) — сбрасывает к пустому полю
     */
    fun clearFormula() {
        saveStateForUndo("Очистка формулы")
        AppLogger.log("ACTION", "Очистка формулы")
        boundsRegistry.clear()
        _state.update {
            it.copy(
                elements = emptyList(),
                variableValues = emptyMap(),
                calculationResult = null,
                calculationError = null
            )
        }
    }

    // ===== Меню оператора =====

    /**
     * Клик на ellipsis — показать меню выбора оператора
     */
    fun onEllipsisClick(id: String) {
        AppLogger.userTap("ellipsis", "id=$id")
        AppLogger.dialogOpened("OperatorMenu", "для $id")
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
        saveStateForUndo("Выбор оператора ${type.name}")
        AppLogger.userSelectOperator(type.name, targetId)

        _state.update {
            it.copy(
                elements = it.elements.replaceEllipsis(targetId, type),
                showOperatorMenu = false,
                operatorMenuTargetId = null
            )
        }
        AppLogger.formulaChanged(_state.value.elements.toLogString())
    }

    /**
     * Закрытие меню оператора
     */
    fun dismissOperatorMenu() {
        _state.update {
            it.copy(
                showOperatorMenu = false,
                operatorMenuTargetId = null,
                isOperatorReplaceMode = false
            )
        }
    }

    /**
     * Клик на существующий оператор — показать меню для замены
     */
    fun onOperatorClick(id: String) {
        AppLogger.userTap("operator", "id=$id")
        AppLogger.dialogOpened("OperatorMenu", "замена для $id")
        _state.update {
            it.copy(
                showOperatorMenu = true,
                operatorMenuTargetId = id,
                isOperatorReplaceMode = true
            )
        }
    }

    /**
     * Выбор оператора для замены существующего
     */
    fun replaceOperator(type: OperatorType) {
        val targetId = _state.value.operatorMenuTargetId ?: return
        saveStateForUndo("Замена оператора на ${type.name}")
        AppLogger.log("ACTION", "Замена оператора $targetId на ${type.name}")

        _state.update {
            it.copy(
                elements = it.elements.replaceOperator(targetId, type),
                showOperatorMenu = false,
                operatorMenuTargetId = null,
                isOperatorReplaceMode = false
            )
        }
        AppLogger.formulaChanged(_state.value.elements.toLogString())
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
        saveStateForUndo("Добавление ${preset.name}")
        Log.d("FormulaEditor", "dropPreset called: ${preset.name}")
        AppLogger.userDropPreset(preset.name)

        // Конвертируем preset в элементы (только правая часть)
        val newElements = preset.toFormulaElements()
        Log.d("FormulaEditor", "Converted to ${newElements.size} elements")

        _state.update { currentState ->
            // Добавляем к существующей формуле с автоматическим ellipsis
            val updatedElements = currentState.elements.appendElements(newElements)
            Log.d("FormulaEditor", "Total elements now: ${updatedElements.size}")

            currentState.copy(elements = updatedElements)
        }
        AppLogger.formulaChanged(_state.value.elements.toLogString())
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

    /**
     * Добавить формулу по двойному тапу — добавляет ПРАВУЮ часть к текущей формуле
     * Например: текущая "m × a", двойной тап на "v = s ÷ t" → "m × a [оператор] s ÷ t"
     */
    fun loadPresetFormula(preset: PresetFormula) {
        saveStateForUndo("Добавление формулы ${preset.name}")
        Log.d("FormulaEditor", "loadPresetFormula (append): ${preset.name}")
        AppLogger.log("ACTION", "Добавление формулы: ${preset.name}")

        // Конвертируем preset в элементы (только ПРАВАЯ часть, как при drag & drop)
        val newElements = preset.toFormulaElements()
        Log.d("FormulaEditor", "Converted to ${newElements.size} elements")

        _state.update { currentState ->
            // Добавляем к существующей формуле с автоматическим ellipsis
            val updatedElements = currentState.elements.appendElements(newElements)
            Log.d("FormulaEditor", "Total elements now: ${updatedElements.size}")

            currentState.copy(elements = updatedElements)
        }
        AppLogger.formulaChanged(_state.value.elements.toLogString())
    }

    // ===== Ввод значений переменных =====

    /**
     * Клик на переменную для ввода значения (короткий тап)
     */
    fun onVariableClickForValue(id: String) {
        val element = _state.value.elements.findById(id)
        if (element is FormulaElement.Variable) {
            AppLogger.userTap("переменная", "${element.displayValue} (id=$id)")
            AppLogger.dialogOpened("VariableInput", "для ${element.displayValue}")
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
     * Сохранить значение переменной по ID
     */
    fun setVariableValue(variableId: String, value: Double?) {
        val varName = _state.value.variableInputName
        saveStateForUndo("Значение $varName = $value")
        AppLogger.userInputValue(varName, variableId, value)

        _state.update { state ->
            val newValues = if (value != null) {
                state.variableValues + (variableId to value)
            } else {
                state.variableValues - variableId
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

        // Собираем все ID переменных из формулы (кроме констант)
        val allVariableIds = collectVariableIds(state.elements)

        // Проверяем, все ли переменные заданы
        val missingVariables = allVariableIds.filter {
            !state.variableValues.containsKey(it)
        }

        if (missingVariables.isNotEmpty()) {
            AppLogger.calculationMissing(missingVariables.toSet())
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
            AppLogger.calculationStarted(state.elements.toLogString(), state.variableValues)

            // Простое вычисление (можно заменить на полноценный парсер)
            val result = evaluateSimple(formulaString)
            AppLogger.calculationResult(result, formulaString)

            // Сохраняем в историю
            calculationHistory.addEntry(
                formulaDescription = state.elements.toLogString(),
                result = result,
                variables = state.variableValues
            )

            _state.update {
                it.copy(
                    calculationResult = result,
                    calculationError = null,
                    calculationHistory = calculationHistory.getAll()
                )
            }
        } catch (e: Exception) {
            Log.e("Calculator", "Calculation error", e)
            AppLogger.calculationError(e.message ?: "Unknown error", state.elements.toLogString())
            _state.update {
                it.copy(
                    calculationResult = null,
                    calculationError = e.message
                )
            }
        }
    }

    /**
     * Собрать все ID переменных из формулы (кроме констант)
     */
    private fun collectVariableIds(elements: List<FormulaElement>): Set<String> {
        val result = mutableSetOf<String>()
        for (element in elements) {
            when (element) {
                is FormulaElement.Variable -> {
                    // Константы не требуют ввода значений
                    if (!isConstant(element.displayValue)) {
                        result.add(element.id)
                    }
                }
                is FormulaElement.Fraction -> {
                    result.addAll(collectVariableIds(element.numerator))
                    result.addAll(collectVariableIds(element.denominator))
                }
                is FormulaElement.Parentheses -> {
                    result.addAll(collectVariableIds(element.children))
                }
                else -> {}
            }
        }
        return result
    }

    // ===== Скобки =====

    /**
     * Обернуть элемент в скобки
     */
    fun wrapInParentheses(targetId: String) {
        saveStateForUndo("Обернуть в скобки")
        AppLogger.log("ACTION", "Обёртывание в скобки: $targetId")

        _state.update {
            it.copy(elements = it.elements.wrapInParentheses(targetId))
        }
        AppLogger.formulaChanged(_state.value.elements.toLogString())
    }

    /**
     * Развернуть скобки — убрать скобки, оставив содержимое
     */
    fun unwrapParentheses(targetId: String) {
        saveStateForUndo("Развернуть скобки")
        AppLogger.log("ACTION", "Разворачивание скобок: $targetId")

        _state.update {
            it.copy(elements = it.elements.unwrapParentheses(targetId))
        }
        AppLogger.formulaChanged(_state.value.elements.toLogString())
    }

    /**
     * Клик на скобки — открыть диалог
     */
    fun onParenthesesClick(id: String) {
        AppLogger.userTap("скобки", "id=$id")
        AppLogger.dialogOpened("ParenthesesDialog", "для $id")
        _state.update {
            it.copy(
                showParenthesesDialog = true,
                parenthesesDialogTargetId = id
            )
        }
    }

    /**
     * Закрыть диалог скобок
     */
    fun dismissParenthesesDialog() {
        _state.update {
            it.copy(
                showParenthesesDialog = false,
                parenthesesDialogTargetId = null
            )
        }
    }

    /**
     * Проверить, является ли имя константой
     */
    private fun isConstant(name: String): Boolean {
        return name in listOf("π", "e", "c", "G", "φ")
    }

    /**
     * Конвертировать элементы в строку для вычисления
     * values - Map с ID переменной как ключ
     */
    private fun elementsToString(
        elements: List<FormulaElement>,
        values: Map<String, Double>
    ): String {
        val sb = StringBuilder()
        for (element in elements) {
            when (element) {
                is FormulaElement.Variable -> {
                    // Сначала пробуем по ID, потом константу
                    val value = values[element.id] ?: getConstantValue(element.displayValue) ?: 1.0
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
                is FormulaElement.Parentheses -> {
                    sb.append("(")
                    sb.append(elementsToString(element.children, values))
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