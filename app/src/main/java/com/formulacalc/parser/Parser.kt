package com.formulacalc.parser

import com.formulacalc.model.FormulaToken

/**
 * Рекурсивный нисходящий парсер для математических выражений.
 *
 * Приоритеты операторов (от низшего к высшему):
 * 1. = (присваивание)
 * 2. +, - (сложение, вычитание)
 * 3. *, / (умножение, деление)
 * 4. ^ (степень, правоассоциативный)
 * 5. унарные операции (-, √)
 * 6. функции (sin, cos, tan, ln, log, abs, round)
 * 7. скобки и атомы (числа, переменные)
 */
class Parser(tokens: List<FormulaToken>) {
    private val lexerTokens = EnhancedLexer(tokens).tokenize()
    private var position = 0

    private val currentToken: LexerToken
        get() = lexerTokens.getOrElse(position) { LexerToken.EOF }

    /**
     * Парсинг формулы в AST
     */
    fun parse(): ParseResult {
        return try {
            val ast = parseExpression()
            if (currentToken != LexerToken.EOF) {
                ParseResult.Error("Неожиданный токен: $currentToken")
            } else {
                ParseResult.Success(ast)
            }
        } catch (e: Exception) {
            ParseResult.Error(e.message ?: "Ошибка парсинга")
        }
    }

    /**
     * expression = assignment | additive
     */
    private fun parseExpression(): ASTNode {
        // Проверяем, есть ли присваивание (var = expr)
        if (currentToken is LexerToken.Variable) {
            val varToken = currentToken as LexerToken.Variable
            val savedPosition = position
            advance()

            if (currentToken == LexerToken.Equals) {
                advance()
                val expr = parseAdditive()
                return ASTNode.AssignmentNode(varToken.name, expr)
            } else {
                // Откатываемся, это не присваивание
                position = savedPosition
            }
        }

        return parseAdditive()
    }

    /**
     * additive = multiplicative (('+' | '-') multiplicative)*
     */
    private fun parseAdditive(): ASTNode {
        var left = parseMultiplicative()

        while (currentToken is LexerToken.Operator) {
            val op = (currentToken as LexerToken.Operator).symbol
            if (op == "+" || op == "-") {
                advance()
                val right = parseMultiplicative()
                left = ASTNode.BinaryOpNode(op, left, right)
            } else {
                break
            }
        }

        return left
    }

    /**
     * multiplicative = power (('*' | '/') power)*
     */
    private fun parseMultiplicative(): ASTNode {
        var left = parsePower()

        while (currentToken is LexerToken.Operator) {
            val op = (currentToken as LexerToken.Operator).symbol
            if (op == "*" || op == "/") {
                advance()
                val right = parsePower()
                left = ASTNode.BinaryOpNode(op, left, right)
            } else {
                break
            }
        }

        return left
    }

    /**
     * power = unary ('^' power)?  (правоассоциативный)
     */
    private fun parsePower(): ASTNode {
        val left = parseUnary()

        if (currentToken is LexerToken.Operator &&
            (currentToken as LexerToken.Operator).symbol == "^"
        ) {
            advance()
            val right = parsePower() // Рекурсия для правоассоциативности
            return ASTNode.BinaryOpNode("^", left, right)
        }

        return left
    }

    /**
     * unary = '-' unary | '√' unary | function | atom
     */
    private fun parseUnary(): ASTNode {
        if (currentToken is LexerToken.Operator) {
            val op = (currentToken as LexerToken.Operator).symbol
            if (op == "-") {
                advance()
                val operand = parseUnary()
                return ASTNode.UnaryOpNode("-", operand)
            }
        }

        if (currentToken is LexerToken.Function) {
            val func = currentToken as LexerToken.Function
            if (func.name == "sqrt") {
                advance()
                val operand = parseUnary()
                return ASTNode.FunctionNode("sqrt", operand)
            }
        }

        return parseFunction()
    }

    /**
     * function = ('sin' | 'cos' | 'tan' | 'ln' | 'log' | 'abs' | 'round' | 'sqrt') '(' expression ')' | atom
     */
    private fun parseFunction(): ASTNode {
        if (currentToken is LexerToken.Function) {
            val func = currentToken as LexerToken.Function
            advance()

            // Функция может быть с скобками или без
            val argument = if (currentToken == LexerToken.LeftParen) {
                advance() // пропускаем '('
                val arg = parseExpression()
                expect(LexerToken.RightParen, "Ожидается ')'")
                arg
            } else {
                // Без скобок — следующий атом
                parseAtom()
            }

            return ASTNode.FunctionNode(func.name, argument)
        }

        return parseAtom()
    }

    /**
     * atom = number | variable | '(' expression ')'
     */
    private fun parseAtom(): ASTNode {
        return when (val token = currentToken) {
            is LexerToken.Number -> {
                advance()
                ASTNode.NumberNode(token.value)
            }

            is LexerToken.Variable -> {
                advance()
                ASTNode.VariableNode(token.name)
            }

            LexerToken.LeftParen -> {
                advance()
                val expr = parseExpression()
                expect(LexerToken.RightParen, "Ожидается ')'")
                expr
            }

            else -> {
                throw ParseException("Неожиданный токен: $token")
            }
        }
    }

    private fun advance() {
        if (position < lexerTokens.size - 1) {
            position++
        }
    }

    private fun expect(expected: LexerToken, message: String) {
        if (currentToken != expected && currentToken::class != expected::class) {
            throw ParseException(message)
        }
        advance()
    }
}

/**
 * Результат парсинга
 */
sealed class ParseResult {
    data class Success(val ast: ASTNode) : ParseResult()
    data class Error(val message: String) : ParseResult()
}

/**
 * Исключение парсинга
 */
class ParseException(message: String) : Exception(message)
