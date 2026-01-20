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

fun createParentheses(
    children: List<FormulaElement> = emptyList()
): FormulaElement.Parentheses {
    return FormulaElement.Parentheses(children = children)
}

// ===== Начальная пустая формула =====

/**
 * Начальная пустая формула — пустой список.
 * Пользователь перетаскивает формулы из нижней панели.
 */
fun getInitialGravityFormula(): List<FormulaElement> {
    return emptyList()
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
        is FormulaElement.Parentheses -> this.copy(
            id = java.util.UUID.randomUUID().toString(),
            children = children.map { it.clone() }
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
        if (element is FormulaElement.Parentheses) {
            element.children.findById(id)?.let { return it }
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

            val numHasContent = newNumerator.any { it is FormulaElement.Variable || it is FormulaElement.Fraction || it is FormulaElement.Parentheses }
            val denHasContent = newDenominator.any { it is FormulaElement.Variable || it is FormulaElement.Fraction || it is FormulaElement.Parentheses }

            when {
                !numHasContent && !denHasContent -> {
                    // Обе части пусты — пропускаем дробь
                }
                !numHasContent && denHasContent -> {
                    // Числитель пуст — заменяем дробь знаменателем
                    result.addAll(newDenominator.filter { it is FormulaElement.Variable || it is FormulaElement.Fraction || it is FormulaElement.Parentheses })
                }
                numHasContent && !denHasContent -> {
                    // Знаменатель пуст — заменяем дробь числителем
                    result.addAll(newNumerator.filter { it is FormulaElement.Variable || it is FormulaElement.Fraction || it is FormulaElement.Parentheses })
                }
                else -> {
                    // Обе части содержат элементы — сохраняем дробь
                    result.add(element.copy(numerator = newNumerator, denominator = newDenominator))
                }
            }
        } else if (element is FormulaElement.Parentheses) {
            val newChildren = element.children.removeById(id).cleanOrphanedOperators()
            val hasContent = newChildren.any { it is FormulaElement.Variable || it is FormulaElement.Fraction || it is FormulaElement.Parentheses }

            if (hasContent) {
                // Есть содержимое — сохраняем скобки
                result.add(element.copy(children = newChildren))
            } else if (newChildren.isEmpty()) {
                // Скобки пусты — удаляем их
            } else {
                // Только операторы — удаляем скобки
            }
        } else {
            result.add(element)
        }
    }

    return result
}

// ===== Вспомогательные функции для работы со скобками и операторами =====

private fun FormulaElement.Operator.isParenthesis(): Boolean =
    type == OperatorType.OPEN_PAREN || type == OperatorType.CLOSE_PAREN

private fun FormulaElement.Operator.isMathOperator(): Boolean =
    type == OperatorType.PLUS || type == OperatorType.MINUS ||
    type == OperatorType.MULTIPLY || type == OperatorType.DIVIDE

// ===== Очистка "сиротских" операторов =====

private fun List<FormulaElement>.cleanOrphanedOperators(): List<FormulaElement> {
    val result = mutableListOf<FormulaElement>()

    for (element in this) {
        val prev = result.lastOrNull()

        // Скобки всегда добавляем
        if (element is FormulaElement.Operator && element.isParenthesis()) {
            result.add(element)
            continue
        }

        // Пропускаем математические операторы/ellipsis в начале
        if (result.isEmpty() && (element is FormulaElement.Ellipsis ||
            (element is FormulaElement.Operator && element.isMathOperator()))) {
            continue
        }

        // Пропускаем повторяющиеся математические операторы
        val elementIsMathOp = element is FormulaElement.Operator && element.isMathOperator()
        val elementIsEllipsis = element is FormulaElement.Ellipsis
        val prevIsMathOp = prev is FormulaElement.Operator && prev.isMathOperator()
        val prevIsEllipsis = prev is FormulaElement.Ellipsis

        if ((elementIsMathOp || elementIsEllipsis) && (prevIsMathOp || prevIsEllipsis)) {
            // Заменяем на ellipsis
            result[result.lastIndex] = createEllipsis()
            continue
        }

        result.add(element)
    }

    // Удаляем trailing математические операторы/ellipsis (но НЕ скобки)
    while (result.isNotEmpty()) {
        val last = result.last()
        val shouldRemove = last is FormulaElement.Ellipsis ||
                          (last is FormulaElement.Operator && last.isMathOperator())
        if (shouldRemove) {
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
                // Добавляем ellipsis между переменной/дробью/скобками и дробью
                if (prev is FormulaElement.Variable || prev is FormulaElement.Fraction || prev is FormulaElement.Parentheses) {
                    result.add(createEllipsis())
                }
                // Добавляем ellipsis после закрывающей скобки перед дробью
                if (prev is FormulaElement.Operator && prev.type == OperatorType.CLOSE_PAREN) {
                    result.add(createEllipsis())
                }
                result.add(
                    element.copy(
                        numerator = element.numerator.normalize(),
                        denominator = element.denominator.normalize()
                    )
                )
            }

            is FormulaElement.Parentheses -> {
                // Добавляем ellipsis между переменной/дробью/скобками и скобками
                if (prev is FormulaElement.Variable || prev is FormulaElement.Fraction || prev is FormulaElement.Parentheses) {
                    result.add(createEllipsis())
                }
                // Добавляем ellipsis после закрывающей скобки
                if (prev is FormulaElement.Operator && prev.type == OperatorType.CLOSE_PAREN) {
                    result.add(createEllipsis())
                }
                result.add(element.copy(children = element.children.normalize()))
            }

            is FormulaElement.Operator -> {
                if (element.isParenthesis()) {
                    // Скобки — особая обработка
                    if (element.type == OperatorType.OPEN_PAREN) {
                        // Открывающая скобка после переменной/дроби/закрывающей скобки — добавляем ellipsis
                        if (prev is FormulaElement.Variable || prev is FormulaElement.Fraction || prev is FormulaElement.Parentheses ||
                            (prev is FormulaElement.Operator && prev.type == OperatorType.CLOSE_PAREN)) {
                            result.add(createEllipsis())
                        }
                    }
                    result.add(element)
                } else {
                    // Математические операторы — пропускаем повторяющиеся
                    val prevIsMathOp = prev is FormulaElement.Operator && prev.isMathOperator()
                    val prevIsEllipsis = prev is FormulaElement.Ellipsis

                    if (prevIsMathOp || prevIsEllipsis) {
                        if (prevIsMathOp) {
                            result[result.lastIndex] = createEllipsis()
                        }
                    } else {
                        result.add(element)
                    }
                }
            }

            is FormulaElement.Ellipsis -> {
                // Пропускаем orphaned ellipsis (не после переменной/дроби/скобок)
                val validPrev = prev is FormulaElement.Variable ||
                                prev is FormulaElement.Fraction ||
                                prev is FormulaElement.Parentheses ||
                                (prev is FormulaElement.Operator && prev.type == OperatorType.CLOSE_PAREN)
                if (!validPrev) {
                    continue
                }
                result.add(element)
            }

            is FormulaElement.Variable -> {
                // Добавляем ellipsis между двумя переменными/дробями/скобками
                if (prev is FormulaElement.Variable || prev is FormulaElement.Fraction || prev is FormulaElement.Parentheses) {
                    result.add(createEllipsis())
                }
                // Добавляем ellipsis после закрывающей скобки перед переменной
                if (prev is FormulaElement.Operator && prev.type == OperatorType.CLOSE_PAREN) {
                    result.add(createEllipsis())
                }
                result.add(element)
            }

            is FormulaElement.Equals -> {
                result.add(element)
            }
        }
    }

    // Удаляем trailing математические операторы/ellipsis (но НЕ скобки)
    while (result.isNotEmpty()) {
        val last = result.last()
        val shouldRemove = last is FormulaElement.Ellipsis ||
                          (last is FormulaElement.Operator && last.isMathOperator())
        if (shouldRemove) {
            result.removeLast()
        } else {
            break
        }
    }

    // Удаляем leading математические операторы/ellipsis (но НЕ скобки)
    while (result.isNotEmpty()) {
        val first = result.first()
        val shouldRemove = first is FormulaElement.Ellipsis ||
                          (first is FormulaElement.Operator && first.isMathOperator())
        if (shouldRemove) {
            result.removeAt(0)
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
        } else if (el is FormulaElement.Parentheses) {
            // Рекурсивно ищем в скобках
            val newChildren = el.children.insertHorizontal(element, targetId, side)

            if (newChildren != el.children) {
                result.add(el.copy(children = newChildren))
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
        } else if (el is FormulaElement.Parentheses) {
            // Рекурсивно ищем в скобках
            val newChildren = el.children.insertVertical(element, targetId, side)

            if (newChildren != el.children) {
                result.add(el.copy(children = newChildren))
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
            element is FormulaElement.Parentheses -> {
                element.copy(children = element.children.updateExponent(targetId, exponent))
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
            element is FormulaElement.Parentheses -> {
                element.copy(children = element.children.replaceEllipsis(targetId, operatorType))
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
 * Автоматически вставляет ellipsis между формулами через normalize().
 *
 * Если формула пуста — просто добавляем новые элементы.
 * Если уже есть элементы — добавляем новые и нормализуем.
 */
fun List<FormulaElement>.appendElements(newElements: List<FormulaElement>): List<FormulaElement> {
    if (newElements.isEmpty()) return this
    if (this.isEmpty()) return newElements.normalize()

    // Проверяем, есть ли значимые элементы
    val hasContent = this.any {
        it is FormulaElement.Variable || it is FormulaElement.Fraction || it is FormulaElement.Parentheses
    }

    return if (hasContent) {
        // Уже есть элементы — добавляем и нормализуем (ellipsis добавится автоматически)
        (this + newElements).normalize()
    } else {
        // Нет значимых элементов — просто добавляем
        newElements.normalize()
    }
}

// ===== Обёртывание в скобки =====

/**
 * Обернуть элемент по ID в скобки.
 * Находит элемент, заменяет его на Parentheses(children = [element])
 */
fun List<FormulaElement>.wrapInParentheses(targetId: String): List<FormulaElement> {
    return map { element ->
        when {
            element.id == targetId -> {
                // Нашли элемент — оборачиваем в скобки
                createParentheses(children = listOf(element.clone()))
            }
            element is FormulaElement.Fraction -> {
                element.copy(
                    numerator = element.numerator.wrapInParentheses(targetId),
                    denominator = element.denominator.wrapInParentheses(targetId)
                )
            }
            element is FormulaElement.Parentheses -> {
                element.copy(children = element.children.wrapInParentheses(targetId))
            }
            else -> element
        }
    }
}

/**
 * Обернуть диапазон элементов в скобки (по индексам).
 * Используется для выделения нескольких элементов.
 */
fun List<FormulaElement>.wrapRangeInParentheses(startIndex: Int, endIndex: Int): List<FormulaElement> {
    if (startIndex < 0 || endIndex >= this.size || startIndex > endIndex) {
        return this
    }

    val result = mutableListOf<FormulaElement>()

    // Элементы до выделения
    result.addAll(this.subList(0, startIndex))

    // Выделенные элементы — оборачиваем в скобки
    val selectedElements = this.subList(startIndex, endIndex + 1)
    result.add(createParentheses(children = selectedElements.map { it.clone() }))

    // Элементы после выделения
    if (endIndex + 1 < this.size) {
        result.addAll(this.subList(endIndex + 1, this.size))
    }

    return result.normalize()
}

/**
 * Заменить оператор на другой тип.
 * Находит Operator по ID и меняет его type.
 */
fun List<FormulaElement>.replaceOperator(targetId: String, newType: OperatorType): List<FormulaElement> {
    return map { element ->
        when {
            element.id == targetId && element is FormulaElement.Operator -> {
                element.copy(type = newType)
            }
            element is FormulaElement.Fraction -> {
                element.copy(
                    numerator = element.numerator.replaceOperator(targetId, newType),
                    denominator = element.denominator.replaceOperator(targetId, newType)
                )
            }
            element is FormulaElement.Parentheses -> {
                element.copy(children = element.children.replaceOperator(targetId, newType))
            }
            else -> element
        }
    }
}

/**
 * Развернуть скобки — убрать скобки, оставив содержимое.
 * Находит Parentheses по ID и заменяет их на children.
 */
fun List<FormulaElement>.unwrapParentheses(targetId: String): List<FormulaElement> {
    val result = mutableListOf<FormulaElement>()

    for (element in this) {
        when {
            element.id == targetId && element is FormulaElement.Parentheses -> {
                // Нашли скобки — заменяем на содержимое
                result.addAll(element.children.map { it.clone() })
            }
            element is FormulaElement.Fraction -> {
                result.add(element.copy(
                    numerator = element.numerator.unwrapParentheses(targetId),
                    denominator = element.denominator.unwrapParentheses(targetId)
                ))
            }
            element is FormulaElement.Parentheses -> {
                result.add(element.copy(children = element.children.unwrapParentheses(targetId)))
            }
            else -> result.add(element)
        }
    }

    return result.normalize()
}

/**
 * Добавить элемент внутрь скобок.
 * Находит Parentheses по ID и добавляет element в children.
 */
fun List<FormulaElement>.addToParentheses(parenthesesId: String, element: FormulaElement): List<FormulaElement> {
    return map { el ->
        when {
            el.id == parenthesesId && el is FormulaElement.Parentheses -> {
                // Нашли скобки — добавляем элемент внутрь
                el.copy(children = (el.children + element).normalize())
            }
            el is FormulaElement.Fraction -> {
                el.copy(
                    numerator = el.numerator.addToParentheses(parenthesesId, element),
                    denominator = el.denominator.addToParentheses(parenthesesId, element)
                )
            }
            el is FormulaElement.Parentheses -> {
                el.copy(children = el.children.addToParentheses(parenthesesId, element))
            }
            else -> el
        }
    }
}

// ===== Логирование =====

/**
 * Конвертирует элемент в строку для логирования
 */
fun FormulaElement.toLogString(): String {
    return when (this) {
        is FormulaElement.Variable -> {
            val exp = exponent?.let {
                when (it) {
                    is Exponent.Simple -> "^${it.value}"
                    is Exponent.Fraction -> "^(${it.numerator}/${it.denominator})"
                }
            } ?: ""
            "Var($displayValue$exp)"
        }
        is FormulaElement.Operator -> "Op(${type.symbol})"
        is FormulaElement.Equals -> "Eq(=)"
        is FormulaElement.Ellipsis -> "Ellipsis(···)"
        is FormulaElement.Fraction -> {
            val num = numerator.joinToString(" ") { it.toLogString() }
            val den = denominator.joinToString(" ") { it.toLogString() }
            "Frac[$num / $den]"
        }
        is FormulaElement.Parentheses -> {
            val content = children.joinToString(" ") { it.toLogString() }
            "Paren($content)"
        }
    }
}

/**
 * Конвертирует список элементов в строку для логирования
 */
fun List<FormulaElement>.toLogString(): String {
    return joinToString(" ") { it.toLogString() }
}
