package com.formulacalc.model

/**
 * Готовая формула с названием и описанием
 */
data class PresetFormula(
    val name: String,
    val description: String,
    val tokens: List<FormulaToken>
) {
    /**
     * Получить отображаемый текст формулы
     */
    fun toDisplayString(): String {
        return tokens.joinToString(" ") { it.displayText }
    }
}

/**
 * Список готовых формул для вкладки "Формулы"
 */
val presetFormulas = listOf(
    // F = m · a (Второй закон Ньютона)
    PresetFormula(
        name = "Второй закон Ньютона",
        description = "Сила равна произведению массы на ускорение",
        tokens = listOf(
            FormulaToken.Variable("F"),
            FormulaToken.Operator.EQUALS,
            FormulaToken.Variable("m"),
            FormulaToken.Operator.MULTIPLY,
            FormulaToken.Variable("a")
        )
    ),

    // v = s / t (Скорость)
    PresetFormula(
        name = "Скорость",
        description = "Скорость равна пути, делённому на время",
        tokens = listOf(
            FormulaToken.Variable("v"),
            FormulaToken.Operator.EQUALS,
            FormulaToken.Variable("s"),
            FormulaToken.Operator.DIVIDE,
            FormulaToken.Variable("t")
        )
    ),

    // E = m · c² (Эквивалентность массы и энергии)
    PresetFormula(
        name = "E = mc²",
        description = "Энергия равна массе, умноженной на квадрат скорости света",
        tokens = listOf(
            FormulaToken.Variable("E"),
            FormulaToken.Operator.EQUALS,
            FormulaToken.Variable("m"),
            FormulaToken.Operator.MULTIPLY,
            FormulaToken.Superscript("c", "²")
        )
    ),

    // F = G · m₁ · m₂ / r² (Закон всемирного тяготения)
    PresetFormula(
        name = "Закон всемирного тяготения",
        description = "Сила гравитационного притяжения между двумя телами",
        tokens = listOf(
            FormulaToken.Variable("F"),
            FormulaToken.Operator.EQUALS,
            FormulaToken.Variable("G"),
            FormulaToken.Operator.MULTIPLY,
            FormulaToken.Subscript("m", "₁"),
            FormulaToken.Operator.MULTIPLY,
            FormulaToken.Subscript("m", "₂"),
            FormulaToken.Operator.DIVIDE,
            FormulaToken.Superscript("r", "²")
        )
    ),

    // p = F / S (Давление)
    PresetFormula(
        name = "Давление",
        description = "Давление равно силе, делённой на площадь",
        tokens = listOf(
            FormulaToken.Variable("p"),
            FormulaToken.Operator.EQUALS,
            FormulaToken.Variable("F"),
            FormulaToken.Operator.DIVIDE,
            FormulaToken.Variable("S")
        )
    ),

    // a = (v - v₀) / t (Ускорение)
    PresetFormula(
        name = "Ускорение",
        description = "Ускорение равно изменению скорости за время",
        tokens = listOf(
            FormulaToken.Variable("a"),
            FormulaToken.Operator.EQUALS,
            FormulaToken.Parenthesis.OPEN,
            FormulaToken.Variable("v"),
            FormulaToken.Operator.MINUS,
            FormulaToken.Subscript("v", "₀"),
            FormulaToken.Parenthesis.CLOSE,
            FormulaToken.Operator.DIVIDE,
            FormulaToken.Variable("t")
        )
    ),

    // S = π · r² (Площадь круга)
    PresetFormula(
        name = "Площадь круга",
        description = "Площадь равна π, умноженному на квадрат радиуса",
        tokens = listOf(
            FormulaToken.Variable("S"),
            FormulaToken.Operator.EQUALS,
            FormulaToken.Variable.PI,
            FormulaToken.Operator.MULTIPLY,
            FormulaToken.Superscript("r", "²")
        )
    ),

    // V = (4/3) · π · r³ (Объём шара)
    PresetFormula(
        name = "Объём шара",
        description = "Объём шара через радиус",
        tokens = listOf(
            FormulaToken.Variable("V"),
            FormulaToken.Operator.EQUALS,
            FormulaToken.Parenthesis.OPEN,
            FormulaToken.Number("4"),
            FormulaToken.Operator.DIVIDE,
            FormulaToken.Number("3"),
            FormulaToken.Parenthesis.CLOSE,
            FormulaToken.Operator.MULTIPLY,
            FormulaToken.Variable.PI,
            FormulaToken.Operator.MULTIPLY,
            FormulaToken.Superscript("r", "³")
        )
    ),

    // c² = a² + b² (Теорема Пифагора)
    PresetFormula(
        name = "Теорема Пифагора",
        description = "Квадрат гипотенузы равен сумме квадратов катетов",
        tokens = listOf(
            FormulaToken.Superscript("c", "²"),
            FormulaToken.Operator.EQUALS,
            FormulaToken.Superscript("a", "²"),
            FormulaToken.Operator.PLUS,
            FormulaToken.Superscript("b", "²")
        )
    ),

    // sin²(α) + cos²(α) = 1 (Основное тригонометрическое тождество)
    PresetFormula(
        name = "Тригонометрическое тождество",
        description = "Сумма квадратов синуса и косинуса равна 1",
        tokens = listOf(
            FormulaToken.Function.SIN,
            FormulaToken.Superscript("(α)", "²"),
            FormulaToken.Operator.PLUS,
            FormulaToken.Function.COS,
            FormulaToken.Superscript("(α)", "²"),
            FormulaToken.Operator.EQUALS,
            FormulaToken.Number("1")
        )
    )
)
