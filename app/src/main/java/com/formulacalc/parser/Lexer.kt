package com.formulacalc.parser

import com.formulacalc.model.FormulaToken

/**
 * Токен для парсера (внутреннее представление)
 */
sealed class LexerToken {
    data class Number(val value: Double) : LexerToken()
    data class Variable(val name: String) : LexerToken()
    data class Operator(val symbol: String) : LexerToken()
    data class Function(val name: String) : LexerToken()
    object LeftParen : LexerToken()
    object RightParen : LexerToken()
    object Equals : LexerToken()
    object EOF : LexerToken()
}

/**
 * Лексер — преобразует список FormulaToken в поток LexerToken для парсера.
 */
class Lexer(private val tokens: List<FormulaToken>) {
    private var position = 0

    /**
     * Получить все токены для парсера
     */
    fun tokenize(): List<LexerToken> {
        val result = mutableListOf<LexerToken>()

        while (position < tokens.size) {
            val token = tokens[position]
            result.add(convertToken(token))
            position++
        }

        result.add(LexerToken.EOF)
        return result
    }

    private fun convertToken(token: FormulaToken): LexerToken {
        return when (token) {
            is FormulaToken.Number -> {
                LexerToken.Number(token.value.toDoubleOrNull() ?: 0.0)
            }

            is FormulaToken.Operator -> {
                if (token.symbol == "=") {
                    LexerToken.Equals
                } else {
                    LexerToken.Operator(normalizeOperator(token.symbol))
                }
            }

            is FormulaToken.Function -> {
                LexerToken.Function(token.name)
            }

            is FormulaToken.Variable -> {
                LexerToken.Variable(token.name)
            }

            is FormulaToken.Parenthesis -> {
                if (token.isOpen) LexerToken.LeftParen else LexerToken.RightParen
            }

            is FormulaToken.Subscript -> {
                // Для subscript (m₁) — трактуем как переменную
                LexerToken.Variable(token.base + token.subscript)
            }

            is FormulaToken.Superscript -> {
                // Для superscript (c²) — нужно развернуть в c ^ 2
                // Пока просто как переменную, парсер обработает отдельно
                LexerToken.Variable(token.base + token.superscript)
            }
        }
    }

    /**
     * Нормализация операторов (Unicode → ASCII для вычислений)
     */
    private fun normalizeOperator(symbol: String): String {
        return when (symbol) {
            "×" -> "*"
            "÷" -> "/"
            "−" -> "-"
            "·" -> "*"
            else -> symbol
        }
    }
}

/**
 * Расширенный лексер, который также обрабатывает superscript как степень
 */
class EnhancedLexer(private val tokens: List<FormulaToken>) {
    private var position = 0
    private val result = mutableListOf<LexerToken>()

    fun tokenize(): List<LexerToken> {
        while (position < tokens.size) {
            val token = tokens[position]
            processToken(token)
            position++
        }

        result.add(LexerToken.EOF)
        return result
    }

    private fun processToken(token: FormulaToken) {
        when (token) {
            is FormulaToken.Number -> {
                result.add(LexerToken.Number(token.value.toDoubleOrNull() ?: 0.0))
            }

            is FormulaToken.Operator -> {
                if (token.symbol == "=") {
                    result.add(LexerToken.Equals)
                } else {
                    result.add(LexerToken.Operator(normalizeOperator(token.symbol)))
                }
            }

            is FormulaToken.Function -> {
                result.add(LexerToken.Function(token.name))
            }

            is FormulaToken.Variable -> {
                result.add(LexerToken.Variable(token.name))
            }

            is FormulaToken.Parenthesis -> {
                result.add(if (token.isOpen) LexerToken.LeftParen else LexerToken.RightParen)
            }

            is FormulaToken.Subscript -> {
                result.add(LexerToken.Variable(token.base + token.subscript))
            }

            is FormulaToken.Superscript -> {
                // Разворачиваем c² в c ^ 2
                result.add(LexerToken.Variable(token.base))
                result.add(LexerToken.Operator("^"))
                val exponent = parseSuperscriptExponent(token.superscript)
                result.add(LexerToken.Number(exponent))
            }
        }
    }

    private fun normalizeOperator(symbol: String): String {
        return when (symbol) {
            "×" -> "*"
            "÷" -> "/"
            "−" -> "-"
            "·" -> "*"
            else -> symbol
        }
    }

    /**
     * Парсинг надстрочного индекса в число
     */
    private fun parseSuperscriptExponent(superscript: String): Double {
        val normalized = superscript
            .replace("⁰", "0")
            .replace("¹", "1")
            .replace("²", "2")
            .replace("³", "3")
            .replace("⁴", "4")
            .replace("⁵", "5")
            .replace("⁶", "6")
            .replace("⁷", "7")
            .replace("⁸", "8")
            .replace("⁹", "9")
            .replace("⁻", "-")

        return normalized.toDoubleOrNull() ?: 2.0 // По умолчанию квадрат
    }
}
