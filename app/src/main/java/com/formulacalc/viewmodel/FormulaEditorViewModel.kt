package com.formulacalc.viewmodel

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import com.formulacalc.model.*
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
    val currentExponent: Exponent? = null
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

        if (draggedElement != null && targetId != null && side != null && draggedElement.id != targetId) {
            // Удаляем элемент из старой позиции
            val withoutDragged = currentState.elements.removeById(draggedElement.id)

            // Вставляем в новую позицию (клонируем чтобы получить новый ID)
            val clonedElement = draggedElement.clone()
            val newElements = withoutDragged.insertAt(clonedElement, targetId, side)

            _state.update {
                it.copy(
                    elements = newElements,
                    dragState = DragState(),
                    hoverState = HoverState()
                )
            }
        } else {
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
}