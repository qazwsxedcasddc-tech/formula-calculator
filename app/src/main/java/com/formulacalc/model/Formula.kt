package com.formulacalc.model

/**
 * Модель формулы — список токенов с позицией курсора.
 *
 * @param tokens Список токенов, составляющих формулу
 * @param cursorPosition Позиция курсора (0 = перед первым токеном, tokens.size = после последнего)
 */
data class Formula(
    val tokens: List<FormulaToken> = emptyList(),
    val cursorPosition: Int = 0
) {
    /**
     * Вставить токен в текущую позицию курсора
     */
    fun insertToken(token: FormulaToken): Formula {
        val newTokens = tokens.toMutableList().apply {
            add(cursorPosition, token)
        }
        return Formula(newTokens, cursorPosition + 1)
    }

    /**
     * Вставить список токенов в текущую позицию курсора
     */
    fun insertTokens(newTokens: List<FormulaToken>): Formula {
        val resultTokens = tokens.toMutableList().apply {
            addAll(cursorPosition, newTokens)
        }
        return Formula(resultTokens, cursorPosition + newTokens.size)
    }

    /**
     * Удалить токен перед курсором (backspace)
     */
    fun deleteToken(): Formula {
        if (cursorPosition == 0 || tokens.isEmpty()) return this
        val newTokens = tokens.toMutableList().apply {
            removeAt(cursorPosition - 1)
        }
        return Formula(newTokens, cursorPosition - 1)
    }

    /**
     * Очистить формулу
     */
    fun clear(): Formula = Formula()

    /**
     * Переместить курсор влево
     */
    fun moveCursorLeft(): Formula {
        if (cursorPosition == 0) return this
        return copy(cursorPosition = cursorPosition - 1)
    }

    /**
     * Переместить курсор вправо
     */
    fun moveCursorRight(): Formula {
        if (cursorPosition >= tokens.size) return this
        return copy(cursorPosition = cursorPosition + 1)
    }

    /**
     * Установить курсор в конкретную позицию
     */
    fun setCursorPosition(position: Int): Formula {
        val clampedPosition = position.coerceIn(0, tokens.size)
        return copy(cursorPosition = clampedPosition)
    }

    /**
     * Получить строковое представление формулы для отображения
     */
    fun toDisplayString(): String {
        return tokens.joinToString(" ") { it.displayText }
    }

    /**
     * Проверить, пустая ли формула
     */
    fun isEmpty(): Boolean = tokens.isEmpty()

    companion object {
        /**
         * Создать формулу из готового набора токенов
         */
        fun fromTokens(tokens: List<FormulaToken>): Formula {
            return Formula(tokens, tokens.size)
        }
    }
}
