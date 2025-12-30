package com.formulacalc.model

/**
 * Токен формулы — базовый элемент, из которого строится формула.
 * Каждый токен имеет визуальное представление (displayText) для UI.
 */
sealed class FormulaToken {
    abstract val displayText: String

    /**
     * Число (целое или дробное)
     */
    data class Number(val value: String) : FormulaToken() {
        override val displayText: String = value
    }

    /**
     * Оператор: +, −, ×, ÷, ^, =
     */
    data class Operator(val symbol: String) : FormulaToken() {
        override val displayText: String = symbol

        companion object {
            val PLUS = Operator("+")
            val MINUS = Operator("−")
            val MULTIPLY = Operator("×")
            val DIVIDE = Operator("÷")
            val POWER = Operator("^")
            val EQUALS = Operator("=")
        }
    }

    /**
     * Функция: sin, cos, tan, ln, log, abs, round, √
     */
    data class Function(val name: String) : FormulaToken() {
        override val displayText: String = when (name) {
            "sqrt" -> "√"
            else -> name
        }

        companion object {
            val SIN = Function("sin")
            val COS = Function("cos")
            val TAN = Function("tan")
            val LN = Function("ln")
            val LOG = Function("log")
            val ABS = Function("abs")
            val ROUND = Function("round")
            val SQRT = Function("sqrt")
        }
    }

    /**
     * Переменная: x, y, z, α, β, γ, θ, λ, μ, ω, Δ, Σ, π, m, a, F, v, s, t, E, c, G, r, p, S
     */
    data class Variable(val name: String) : FormulaToken() {
        override val displayText: String = name

        companion object {
            // Латинские переменные
            val X = Variable("x")
            val Y = Variable("y")
            val Z = Variable("z")
            val M = Variable("m")
            val A = Variable("a")
            val F = Variable("F")
            val V = Variable("v")
            val S_LOWER = Variable("s")
            val T = Variable("t")
            val E = Variable("E")
            val C = Variable("c")
            val G = Variable("G")
            val R = Variable("r")
            val P = Variable("p")
            val S_UPPER = Variable("S")

            // Греческие символы
            val ALPHA = Variable("α")
            val BETA = Variable("β")
            val GAMMA = Variable("γ")
            val THETA = Variable("θ")
            val LAMBDA = Variable("λ")
            val MU = Variable("μ")
            val OMEGA = Variable("ω")
            val DELTA = Variable("Δ")
            val SIGMA = Variable("Σ")
            val PI = Variable("π")
            val PHI = Variable("φ")
            val PSI = Variable("ψ")
            val EPSILON = Variable("ε")
            val TAU = Variable("τ")
            val RHO = Variable("ρ")
        }
    }

    /**
     * Скобка: открывающая или закрывающая
     */
    data class Parenthesis(val isOpen: Boolean) : FormulaToken() {
        override val displayText: String = if (isOpen) "(" else ")"

        companion object {
            val OPEN = Parenthesis(true)
            val CLOSE = Parenthesis(false)
        }
    }

    /**
     * Подстрочный индекс (для m₁, m₂ и т.д.)
     */
    data class Subscript(val base: String, val subscript: String) : FormulaToken() {
        override val displayText: String = "$base$subscript"
    }

    /**
     * Надстрочный индекс / степень (для c², r² и т.д.)
     */
    data class Superscript(val base: String, val superscript: String) : FormulaToken() {
        override val displayText: String = "$base$superscript"
    }
}

/**
 * Список греческих символов для вкладки "Греческие"
 */
val greekSymbols = listOf(
    FormulaToken.Variable.ALPHA,
    FormulaToken.Variable.BETA,
    FormulaToken.Variable.GAMMA,
    FormulaToken.Variable.THETA,
    FormulaToken.Variable.LAMBDA,
    FormulaToken.Variable.MU,
    FormulaToken.Variable.OMEGA,
    FormulaToken.Variable.DELTA,
    FormulaToken.Variable.SIGMA,
    FormulaToken.Variable.PI,
    FormulaToken.Variable.PHI,
    FormulaToken.Variable.PSI,
    FormulaToken.Variable.EPSILON,
    FormulaToken.Variable.TAU,
    FormulaToken.Variable.RHO
)

/**
 * Список инженерных операторов
 */
val engineeringOperators = listOf(
    FormulaToken.Operator.PLUS,
    FormulaToken.Operator.MINUS,
    FormulaToken.Operator.MULTIPLY,
    FormulaToken.Operator.DIVIDE,
    FormulaToken.Operator.POWER,
    FormulaToken.Parenthesis.OPEN,
    FormulaToken.Parenthesis.CLOSE
)

/**
 * Список инженерных функций
 */
val engineeringFunctions = listOf(
    FormulaToken.Function.SIN,
    FormulaToken.Function.COS,
    FormulaToken.Function.TAN,
    FormulaToken.Function.LN,
    FormulaToken.Function.LOG,
    FormulaToken.Function.ABS,
    FormulaToken.Function.ROUND,
    FormulaToken.Function.SQRT
)
