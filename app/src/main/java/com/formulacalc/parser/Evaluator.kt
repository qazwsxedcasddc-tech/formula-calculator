package com.formulacalc.parser

import kotlin.math.*

/**
 * Вычислитель AST — выполняет математические вычисления.
 *
 * @param variables Карта переменных и их значений. По умолчанию все переменные = 1.0
 */
class Evaluator(
    private val variables: Map<String, Double> = emptyMap()
) {
    // Встроенные константы
    private val constants = mapOf(
        "π" to PI,
        "pi" to PI,
        "e" to E,
        "φ" to 1.618033988749895, // Золотое сечение
        "c" to 299792458.0, // Скорость света (м/с)
        "G" to 6.67430e-11 // Гравитационная постоянная
    )

    /**
     * Вычислить AST и получить результат
     */
    fun evaluate(ast: ASTNode): EvalResult {
        return try {
            val value = evalNode(ast)
            EvalResult.Success(value)
        } catch (e: Exception) {
            EvalResult.Error(e.message ?: "Ошибка вычисления")
        }
    }

    private fun evalNode(node: ASTNode): Double {
        return when (node) {
            is ASTNode.NumberNode -> node.value

            is ASTNode.VariableNode -> {
                // Сначала проверяем пользовательские переменные
                variables[node.name]
                    // Затем константы
                    ?: constants[node.name]
                    // По умолчанию = 1.0
                    ?: 1.0
            }

            is ASTNode.BinaryOpNode -> {
                val left = evalNode(node.left)
                val right = evalNode(node.right)
                evalBinaryOp(node.operator, left, right)
            }

            is ASTNode.UnaryOpNode -> {
                val operand = evalNode(node.operand)
                evalUnaryOp(node.operator, operand)
            }

            is ASTNode.FunctionNode -> {
                val argument = evalNode(node.argument)
                evalFunction(node.name, argument)
            }

            is ASTNode.AssignmentNode -> {
                // Для присваивания — просто вычисляем правую часть
                evalNode(node.expression)
            }
        }
    }

    private fun evalBinaryOp(operator: String, left: Double, right: Double): Double {
        return when (operator) {
            "+" -> left + right
            "-" -> left - right
            "*" -> left * right
            "/" -> {
                if (right == 0.0) {
                    throw EvalException("Деление на ноль")
                }
                left / right
            }
            "^" -> left.pow(right)
            else -> throw EvalException("Неизвестный оператор: $operator")
        }
    }

    private fun evalUnaryOp(operator: String, operand: Double): Double {
        return when (operator) {
            "-" -> -operand
            else -> throw EvalException("Неизвестный унарный оператор: $operator")
        }
    }

    private fun evalFunction(name: String, argument: Double): Double {
        return when (name.lowercase()) {
            "sin" -> sin(argument)
            "cos" -> cos(argument)
            "tan" -> tan(argument)
            "asin" -> asin(argument)
            "acos" -> acos(argument)
            "atan" -> atan(argument)
            "ln" -> {
                if (argument <= 0) {
                    throw EvalException("Логарифм от неположительного числа")
                }
                ln(argument)
            }
            "log" -> {
                if (argument <= 0) {
                    throw EvalException("Логарифм от неположительного числа")
                }
                log10(argument)
            }
            "sqrt" -> {
                if (argument < 0) {
                    throw EvalException("Корень из отрицательного числа")
                }
                sqrt(argument)
            }
            "abs" -> abs(argument)
            "round" -> round(argument)
            "floor" -> floor(argument)
            "ceil" -> ceil(argument)
            "exp" -> exp(argument)
            else -> throw EvalException("Неизвестная функция: $name")
        }
    }
}

/**
 * Результат вычисления
 */
sealed class EvalResult {
    data class Success(val value: Double) : EvalResult() {
        /**
         * Форматирование результата для отображения
         */
        fun formatted(): String {
            return if (value == value.toLong().toDouble()) {
                value.toLong().toString()
            } else {
                String.format("%.6f", value).trimEnd('0').trimEnd('.')
            }
        }
    }

    data class Error(val message: String) : EvalResult()
}

/**
 * Исключение вычисления
 */
class EvalException(message: String) : Exception(message)

/**
 * Удобная функция для вычисления формулы
 */
fun evaluateFormula(
    tokens: List<com.formulacalc.model.FormulaToken>,
    variables: Map<String, Double> = emptyMap()
): EvalResult {
    val parser = Parser(tokens)
    return when (val parseResult = parser.parse()) {
        is ParseResult.Success -> {
            val evaluator = Evaluator(variables)
            evaluator.evaluate(parseResult.ast)
        }
        is ParseResult.Error -> {
            EvalResult.Error(parseResult.message)
        }
    }
}
