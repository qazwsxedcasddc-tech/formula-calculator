package com.formulacalc.model

import java.util.UUID

/**
 * Экспонента — может быть простой (x²) или дробной (x^(a/b))
 */
sealed class Exponent {
    /**
     * Простая экспонента: "2", "n", "abc"
     */
    data class Simple(val value: String) : Exponent()

    /**
     * Дробная экспонента: a/b
     */
    data class Fraction(val numerator: String, val denominator: String) : Exponent()
}

/**
 * Тип оператора
 */
enum class OperatorType(val symbol: String) {
    PLUS("+"),
    MINUS("−"),
    MULTIPLY("×"),
    DIVIDE("÷"),
    OPEN_PAREN("("),
    CLOSE_PAREN(")")
}

/**
 * Элемент формулы — базовый класс для всех элементов редактируемой формулы.
 *
 * Типы элементов:
 * - Variable: переменная с опциональной экспонентой (F, m₁, r²)
 * - Operator: оператор (+, −, ×, ÷, =)
 * - Ellipsis: placeholder для пропущенного оператора (···)
 * - Fraction: дробь с числителем и знаменателем
 */
sealed class FormulaElement {
    abstract val id: String

    /**
     * Переменная с опциональной экспонентой
     */
    data class Variable(
        override val id: String = generateId(),
        val value: String,
        val displayValue: String = value,
        val exponent: Exponent? = null
    ) : FormulaElement()

    /**
     * Оператор
     */
    data class Operator(
        override val id: String = generateId(),
        val type: OperatorType
    ) : FormulaElement() {
        val symbol: String get() = type.symbol
    }

    /**
     * Знак равенства (особый случай — не перемещается)
     */
    data class Equals(
        override val id: String = generateId()
    ) : FormulaElement()

    /**
     * Ellipsis — placeholder для пропущенного оператора
     */
    data class Ellipsis(
        override val id: String = generateId()
    ) : FormulaElement()

    /**
     * Дробь с числителем и знаменателем
     */
    data class Fraction(
        override val id: String = generateId(),
        val numerator: List<FormulaElement>,
        val denominator: List<FormulaElement>
    ) : FormulaElement()

    /**
     * Скобки — контейнер для группировки элементов
     * Содержит список элементов внутри скобок
     */
    data class Parentheses(
        override val id: String = generateId(),
        val children: List<FormulaElement>
    ) : FormulaElement()

    companion object {
        private fun generateId(): String = UUID.randomUUID().toString()
    }
}

/**
 * Позиция drop при drag & drop
 */
enum class DropSide {
    LEFT,   // Вставить слева от элемента
    RIGHT,  // Вставить справа от элемента
    TOP,    // Создать дробь, dragged сверху
    BOTTOM  // Создать дробь, dragged снизу
}

/**
 * Информация о drop
 */
data class DropInfo(
    val targetId: String,
    val side: DropSide
)
