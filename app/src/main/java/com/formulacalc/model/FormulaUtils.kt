package com.formulacalc.model

/**
 * Утилиты для работы с элементами формулы
 */

// ===== Создание элементов =====

fun createVariable(value: String, displayValue: String? = null, exponent: Exponent? = null): FormulaElement.Variable {
    return FormulaElement.Variable(
        value = value,
        displayValue = displayValue ?: value,
        exponent = exponent
    )
}

fun createOperator(type: OperatorType): FormulaElement.Operator {
    return FormulaElement.Operator(type = type)
}

fun createEquals(): FormulaElement.Equals {
    return FormulaElement.Equals()
}

fun createEllipsis(): FormulaElement.Ellipsis {
    return FormulaElement.Ellipsis()
}

fun createFraction(
    numerator: List<FormulaElement>,
    denominator: List<FormulaElement>
): FormulaElement.Fraction {
    return FormulaElement.Fraction(
        numerator = numerator,
        denominator = denominator
    )
}

// ===== Начальная пустая формула =====

/**
 * Начальная пустая формула: просто "F ="
 * Пользователь перетаскивает формулы из нижней панели
 */
fun getInitialGravityFormula(): List<FormulaElement> {
    return listOf(
        createVariable("F"),
        createEquals()
    )
}

/**
 * Пример формулы гравитации (для тестирования)
 */
fun getGravityFormulaExample(): List<FormulaElement> {
    return listOf(
        createVariable("F"),
        createEquals(),
        createFraction(
            numerator = listOf(
                createVariable("G"),
                createOperator(OperatorType.MULTIPLY),
                createVariable("m₁", "m₁"),
                createOperator(OperatorType.MULTIPLY),
                createVariable("m₂", "m₂")
            ),
            denominator = listOf(
                createVariable("r", "r", Exponent.Simple("2"))
            )
        )
    )
}

// ===== Клонирование =====

fun FormulaElement.clone(): FormulaElement {
    return when (this) {
        is FormulaElement.Variable -> this.copy(id = java.util.UUID.randomUUID().toString())
        is FormulaElement.Operator -> this.copy(id = java.util.UUID.randomUUID().toString())
        is FormulaElement.Equals -> this.copy(id = java.util.UUID.randomUUID().toString())
        is FormulaElement.Ellipsis -> this.copy(id = java.util.UUID.randomUUID().toString())
        is FormulaElement.Fraction -> this.copy(
            id = java.util.UUID.randomUUID().toString(),
            numerator = numerator.map { it.clone() },
            denominator = denominator.map { it.clone() }
        )
    }
}

// ===== Поиск элемента по ID =====

fun List<FormulaElement>.findById(id: String): FormulaElement? {
    for (element in this) {
        if (element.id == id) return element
        if (element is FormulaElement.Fraction) {
            element.numerator.findById(id)?.let { return it }
            element.denominator.findById(id)?.let { return it }
        }
    }
    return null
}

// ===== Удаление элемента по ID =====

fun List<FormulaElement>.removeById(id: String): List<FormulaElement> {
    val result = mutableListOf<FormulaElement>()

    for (element in this) {
        if (element.id == id) continue

        if (element is FormulaElement.Fraction) {
            val newNumerator = element.numerator.removeById(id).cleanOrphanedOperators()
            val newDenominator = element.denominator.removeById(id).cleanOrphanedOperators()

            val numHasContent = newNumerator.any { it is FormulaElement.Variable || it is FormulaElement.Fraction }
            val denHasContent = newDenominator.any { it is FormulaElement.Variable || it is FormulaElement.Fraction }

            when {
                !numHasContent && !denHasContent -> {
                    // Обе части пусты — пропускаем дробь
                }
                !numHasContent && denHasContent -> {
                    // Числитель пуст — заменяем дробь знаменателем
                    result.addAll(newDenominator.filter { it is FormulaElement.Variable || it is FormulaElement.Fraction })
                }
                numHasContent && !denHasContent -> {
                    // Знаменатель пуст — заменяем дробь числителем
                    result.addAll(newNumerator.filter { it is FormulaElement.Variable || it is FormulaElement.Fraction })
                }
                else -> {
                    // Обе части содержат элементы — сохраняем дробь
                    result.add(element.copy(numerator = newNumerator, denominator = newDenominator))
                }
            }
        } else {
            result.add(element)
        }
    }

    return result
}

// ===== Очистка "сиротских" операторов =====

private fun List<FormulaElement>.cleanOrphanedOperators(): List<FormulaElement> {
    val result = mutableListOf<FormulaElement>()

    for (element in this) {
        val prev = result.lastOrNull()

        // Пропускаем операторы/ellipsis в начале (кроме =)
        if (result.isEmpty() && (element is FormulaElement.Operator || element is FormulaElement.Ellipsis)) {
            continue
        }

        // Пропускаем повторяющиеся операторы
        if ((element is FormulaElement.Operator || element is FormulaElement.Ellipsis) &&
            (prev is FormulaElement.Operator || prev is FormulaElement.Ellipsis)
        ) {
            // Заменяем на ellipsis
            result[result.lastIndex] = createEllipsis()
            continue
        }

        result.add(element)
    }

    // Удаляем trailing операторы
    while (result.isNotEmpty()) {
        val last = result.last()
        if (last is FormulaElement.Operator || last is FormulaElement.Ellipsis) {
            result.removeLast()
        } else {
            break
        }
    }

    return result
}

// ===== Нормализация формулы =====

fun List<FormulaElement>.normalize(): List<FormulaElement> {
    val result = mutableListOf<FormulaElement>()

    for (element in this) {
        val prev = result.lastOrNull()

        when (element) {
            is FormulaElement.Fraction -> {
                // Добавляем ellipsis между переменной/дробью и дробью
                if (prev is FormulaElement.Variable || prev is FormulaElement.Fraction) {
                    result.add(createEllipsis())
                }
                result.add(
                    element.copy(
                        numerator = element.numerator.normalize(),
                        denominator = element.denominator.normalize()
                    )
                )
            }

            is FormulaElement.Operator -> {
                // Пропускаем повторяющиеся операторы
                if (prev is FormulaElement.Operator || prev is FormulaElement.Ellipsis) {
                    if (prev is FormulaElement.Operator) {
                        result[result.lastIndex] = createEllipsis()
                    }
                } else {
                    result.add(element)
                }
            }

            is FormulaElement.Ellipsis -> {
                // Пропускаем orphaned ellipsis
                if (prev !is FormulaElement.Variable && prev !is FormulaElement.Fraction) {
                    continue
                }
                result.add(element)
            }

            is FormulaElement.Variable -> {
                // Добавляем ellipsis между двумя переменными/дробями
                if (prev is FormulaElement.Variable || prev is FormulaElement.Fraction) {
                    result.add(createEllipsis())
                }
                result.add(element)
            }

            is FormulaElement.Equals -> {
                result.add(element)
            }
        }
    }

    // Удаляем trailing операторы/ellipsis
    while (result.isNotEmpty()) {
        val last = result.last()
        if (last is FormulaElement.Operator || last is FormulaElement.Ellipsis) {
            result.removeLast()
        } else {
            break
        }
    }

    return result
}

// ===== Вставка элемента =====

fun List<FormulaElement>.insertAt(
    element: FormulaElement,
    targetId: String,
    side: DropSide
): List<FormulaElement> {
    return when (side) {
        DropSide.LEFT, DropSide.RIGHT -> insertHorizontal(element, targetId, side)
        DropSide.TOP, DropSide.BOTTOM -> insertVertical(element, targetId, side)
    }
}

private fun List<FormulaElement>.insertHorizontal(
    element: FormulaElement,
    targetId: String,
    side: DropSide
): List<FormulaElement> {
    val result = mutableListOf<FormulaElement>()

    for ((index, el) in this.withIndex()) {
        if (el.id == targetId) {
            if (side == DropSide.LEFT) {
                result.add(element)
                result.add(el)
            } else {
                result.add(el)
                result.add(element)
            }
        } else if (el is FormulaElement.Fraction) {
            // Рекурсивно ищем в дроби
            val newNumerator = el.numerator.insertHorizontal(element, targetId, side)
            val newDenominator = el.denominator.insertHorizontal(element, targetId, side)

            if (newNumerator != el.numerator || newDenominator != el.denominator) {
                result.add(el.copy(numerator = newNumerator, denominator = newDenominator))
            } else {
                result.add(el)
            }
        } else {
            result.add(el)
        }
    }

    return result.normalize()
}

private fun List<FormulaElement>.insertVertical(
    element: FormulaElement,
    targetId: String,
    side: DropSide
): List<FormulaElement> {
    val result = mutableListOf<FormulaElement>()

    for (el in this) {
        if (el.id == targetId) {
            // Создаём дробь
            val newFraction = if (side == DropSide.TOP) {
                createFraction(
                    numerator = listOf(element.clone()),
                    denominator = listOf(el.clone())
                )
            } else {
                createFraction(
                    numerator = listOf(el.clone()),
                    denominator = listOf(element.clone())
                )
            }
            result.add(newFraction)
        } else if (el is FormulaElement.Fraction) {
            // Рекурсивно ищем в дроби
            val newNumerator = el.numerator.insertVertical(element, targetId, side)
            val newDenominator = el.denominator.insertVertical(element, targetId, side)

            if (newNumerator != el.numerator || newDenominator != el.denominator) {
                result.add(el.copy(numerator = newNumerator, denominator = newDenominator))
            } else {
                result.add(el)
            }
        } else {
            result.add(el)
        }
    }

    return result.normalize()
}

// ===== Обновление экспоненты =====

fun List<FormulaElement>.updateExponent(targetId: String, exponent: Exponent?): List<FormulaElement> {
    return map { element ->
        when {
            element.id == targetId && element is FormulaElement.Variable -> {
                element.copy(exponent = exponent)
            }
            element is FormulaElement.Fraction -> {
                element.copy(
                    numerator = element.numerator.updateExponent(targetId, exponent),
                    denominator = element.denominator.updateExponent(targetId, exponent)
                )
            }
            else -> element
        }
    }
}

// ===== Замена ellipsis на оператор =====

fun List<FormulaElement>.replaceEllipsis(targetId: String, operatorType: OperatorType): List<FormulaElement> {
    return map { element ->
        when {
            element.id == targetId && element is FormulaElement.Ellipsis -> {
                createOperator(operatorType)
            }
            element is FormulaElement.Fraction -> {
                element.copy(
                    numerator = element.numerator.replaceEllipsis(targetId, operatorType),
                    denominator = element.denominator.replaceEllipsis(targetId, operatorType)
                )
            }
            else -> element
        }
    }
}

// ===== Конвертация PresetFormula в FormulaElement =====

/**
 * Нормализует unicode-символы степени в обычные символы.
 * ² → 2, ³ → 3, и т.д.
 */
private fun normalizeExponent(exp: String): String {
    val unicodeToNormal = mapOf(
        '⁰' to '0', '¹' to '1', '²' to '2', '³' to '3', '⁴' to '4',
        '⁵' to '5', '⁶' to '6', '⁷' to '7', '⁸' to '8', '⁹' to '9',
        '⁺' to '+', '⁻' to '-', '⁼' to '=', '⁽' to '(', '⁾' to ')',
        'ⁿ' to 'n', 'ⁱ' to 'i'
    )
    return exp.map { unicodeToNormal[it] ?: it }.joinToString("")
}

/**
 * Конвертирует PresetFormula в список FormulaElement.
 * Возвращает только ПРАВУЮ часть формулы (после знака =).
 *
 * Например: "F = m × a" → [m, ×, a]
 *
 * Также обрабатывает деление как дробь (визуальную).
 */
fun PresetFormula.toFormulaElements(): List<FormulaElement> {
    // Находим индекс знака "="
    val equalsIndex = tokens.indexOfFirst {
        it is FormulaToken.Operator && it.symbol == "="
    }

    // Берём только правую часть (после =)
    val rightSideTokens = if (equalsIndex >= 0 && equalsIndex < tokens.lastIndex) {
        tokens.subList(equalsIndex + 1, tokens.size)
    } else {
        tokens
    }

    // Конвертируем токены в элементы
    return convertTokensToElements(rightSideTokens)
}

/**
 * Конвертирует список токенов в список элементов формулы.
 * Обрабатывает деление как дробь.
 */
private fun convertTokensToElements(tokens: List<FormulaToken>): List<FormulaElement> {
    // Сначала проверяем, есть ли деление — если да, создаём дробь
    val divideIndex = tokens.indexOfFirst {
        it is FormulaToken.Operator && it.symbol == "÷"
    }

    if (divideIndex > 0 && divideIndex < tokens.lastIndex) {
        // Есть деление — создаём дробь
        val numeratorTokens = tokens.subList(0, divideIndex)
        val denominatorTokens = tokens.subList(divideIndex + 1, tokens.size)

        return listOf(
            createFraction(
                numerator = convertSimpleTokens(numeratorTokens),
                denominator = convertSimpleTokens(denominatorTokens)
            )
        )
    }

    // Нет деления — просто конвертируем токены
    return convertSimpleTokens(tokens)
}

/**
 * Конвертирует простые токены (без создания дробей).
 */
private fun convertSimpleTokens(tokens: List<FormulaToken>): List<FormulaElement> {
    val result = mutableListOf<FormulaElement>()

    for (token in tokens) {
        val element = when (token) {
            is FormulaToken.Variable -> {
                createVariable(token.name)
            }
            is FormulaToken.Subscript -> {
                // m₁ → переменная с displayValue
                createVariable(token.base, token.displayText)
            }
            is FormulaToken.Superscript -> {
                // c² → переменная с экспонентой
                // Нормализуем unicode степени в обычные цифры
                val normalizedExponent = normalizeExponent(token.superscript)
                createVariable(token.base, token.base, Exponent.Simple(normalizedExponent))
            }
            is FormulaToken.Operator -> {
                when (token.symbol) {
                    "+" -> createOperator(OperatorType.PLUS)
                    "−" -> createOperator(OperatorType.MINUS)
                    "×" -> createOperator(OperatorType.MULTIPLY)
                    "÷" -> createOperator(OperatorType.DIVIDE)
                    "(" -> createOperator(OperatorType.OPEN_PAREN)
                    ")" -> createOperator(OperatorType.CLOSE_PAREN)
                    else -> null
                }
            }
            is FormulaToken.Parenthesis -> {
                if (token.isOpen) createOperator(OperatorType.OPEN_PAREN)
                else createOperator(OperatorType.CLOSE_PAREN)
            }
            is FormulaToken.Number -> {
                createVariable(token.value)
            }
            is FormulaToken.Function -> {
                createVariable(token.displayText)
            }
        }

        if (element != null) {
            result.add(element)
        }
    }

    return result
}

// ===== Добавление элементов к существующей формуле =====

/**
 * Добавляет новые элементы к существующей формуле.
 * Автоматически вставляет ellipsis между формулами.
 *
 * Если формула была пустая или содержала только "F =" — заменяем всё.
 * Если уже есть элементы — добавляем ellipsis и новые элементы.
 */
fun List<FormulaElement>.appendElements(newElements: List<FormulaElement>): List<FormulaElement> {
    if (newElements.isEmpty()) return this

    // Находим индекс знака "="
    val equalsIndex = this.indexOfFirst { it is FormulaElement.Equals }

    // Берём часть до = (включительно) и часть после =
    val prefix = if (equalsIndex >= 0) {
        this.subList(0, equalsIndex + 1)
    } else {
        emptyList()
    }

    val existingRightSide = if (equalsIndex >= 0 && equalsIndex < this.lastIndex) {
        this.subList(equalsIndex + 1, this.size)
    } else if (equalsIndex < 0) {
        this
    } else {
        emptyList()
    }

    // Проверяем, есть ли значимые элементы в правой части
    val hasContent = existingRightSide.any {
        it is FormulaElement.Variable || it is FormulaElement.Fraction
    }

    return if (hasContent) {
        // Уже есть элементы — добавляем через ellipsis
        (prefix + existingRightSide + newElements).normalize()
    } else {
        // Правая часть пуста — просто добавляем
        (prefix + newElements).normalize()
    }
}
