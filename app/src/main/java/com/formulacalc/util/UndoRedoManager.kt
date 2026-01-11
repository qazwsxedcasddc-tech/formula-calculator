package com.formulacalc.util

import com.formulacalc.model.FormulaElement
import com.formulacalc.model.clone

/**
 * Менеджер Undo/Redo для формул
 * Хранит историю состояний формулы и позволяет отменять/повторять действия
 */
class UndoRedoManager(
    private val maxHistorySize: Int = 50
) {
    // Стек истории (прошлые состояния)
    private val undoStack = mutableListOf<FormulaSnapshot>()

    // Стек для redo (отменённые состояния)
    private val redoStack = mutableListOf<FormulaSnapshot>()

    /**
     * Снимок состояния формулы
     */
    data class FormulaSnapshot(
        val elements: List<FormulaElement>,
        val variableValues: Map<String, Double>,
        val actionName: String // Название действия для отображения
    )

    /**
     * Сохранить текущее состояние перед изменением
     */
    fun saveState(
        elements: List<FormulaElement>,
        variableValues: Map<String, Double>,
        actionName: String
    ) {
        // Добавляем в undo стек
        undoStack.add(FormulaSnapshot(
            elements = elements.map { it.clone() }, // Глубокая копия
            variableValues = variableValues.toMap(),
            actionName = actionName
        ))

        // Ограничиваем размер истории
        while (undoStack.size > maxHistorySize) {
            undoStack.removeAt(0)
        }

        // Очищаем redo стек при новом действии
        redoStack.clear()

        AppLogger.log("UNDO", "Сохранено состояние: $actionName (история: ${undoStack.size})")
    }

    /**
     * Отменить последнее действие
     * @return Предыдущее состояние или null если нечего отменять
     */
    fun undo(
        currentElements: List<FormulaElement>,
        currentVariableValues: Map<String, Double>
    ): FormulaSnapshot? {
        if (undoStack.isEmpty()) {
            AppLogger.log("UNDO", "Нечего отменять")
            return null
        }

        // Сохраняем текущее состояние в redo
        redoStack.add(FormulaSnapshot(
            elements = currentElements.map { it.clone() },
            variableValues = currentVariableValues.toMap(),
            actionName = "Redo"
        ))

        // Возвращаем предыдущее состояние
        val previousState = undoStack.removeLast()
        AppLogger.log("UNDO", "Отменено: ${previousState.actionName}")
        return previousState
    }

    /**
     * Повторить отменённое действие
     * @return Следующее состояние или null если нечего повторять
     */
    fun redo(
        currentElements: List<FormulaElement>,
        currentVariableValues: Map<String, Double>
    ): FormulaSnapshot? {
        if (redoStack.isEmpty()) {
            AppLogger.log("UNDO", "Нечего повторять")
            return null
        }

        // Сохраняем текущее состояние в undo
        undoStack.add(FormulaSnapshot(
            elements = currentElements.map { it.clone() },
            variableValues = currentVariableValues.toMap(),
            actionName = "Undo"
        ))

        // Возвращаем следующее состояние
        val nextState = redoStack.removeLast()
        AppLogger.log("UNDO", "Повторено действие")
        return nextState
    }

    /**
     * Можно ли отменить
     */
    fun canUndo(): Boolean = undoStack.isNotEmpty()

    /**
     * Можно ли повторить
     */
    fun canRedo(): Boolean = redoStack.isNotEmpty()

    /**
     * Очистить историю
     */
    fun clear() {
        undoStack.clear()
        redoStack.clear()
        AppLogger.log("UNDO", "История очищена")
    }

    /**
     * Получить название последнего действия для отмены
     */
    fun getUndoActionName(): String? = undoStack.lastOrNull()?.actionName

    /**
     * Размер истории undo
     */
    fun undoStackSize(): Int = undoStack.size

    /**
     * Размер истории redo
     */
    fun redoStackSize(): Int = redoStack.size
}
