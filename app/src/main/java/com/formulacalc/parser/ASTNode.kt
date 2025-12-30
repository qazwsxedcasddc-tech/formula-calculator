package com.formulacalc.parser

/**
 * Узел абстрактного синтаксического дерева (AST).
 * Представляет структуру математического выражения.
 */
sealed class ASTNode {
    /**
     * Числовой узел
     */
    data class NumberNode(val value: Double) : ASTNode()

    /**
     * Узел переменной
     */
    data class VariableNode(val name: String) : ASTNode()

    /**
     * Бинарная операция (два операнда)
     * Например: a + b, x * y, m ^ 2
     */
    data class BinaryOpNode(
        val operator: String,
        val left: ASTNode,
        val right: ASTNode
    ) : ASTNode()

    /**
     * Унарная операция (один операнд)
     * Например: -x, √x
     */
    data class UnaryOpNode(
        val operator: String,
        val operand: ASTNode
    ) : ASTNode()

    /**
     * Вызов функции
     * Например: sin(x), cos(α), ln(2)
     */
    data class FunctionNode(
        val name: String,
        val argument: ASTNode
    ) : ASTNode()

    /**
     * Узел присваивания (для формул вида F = m * a)
     */
    data class AssignmentNode(
        val variable: String,
        val expression: ASTNode
    ) : ASTNode()
}
