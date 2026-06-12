package dev.s7a.sqldelight.check.api

/**
 * Builder for readable source pattern expressions.
 *
 * Use unary `+` for a required term and unary `-` for an optional term:
 *
 * ```kotlin
 * SqlDialectSourcePatternExpression.build {
 *     +"left"
 *     -"outer"
 *     +"join"
 * }
 * ```
 */
public class SqlDialectSourcePatternExpressionBuilder {
    private val parts = mutableListOf<SqlDialectSourcePatternPart>()

    public operator fun String.unaryPlus() {
        parts += SqlDialectSourcePatternPart.required(this)
    }

    public operator fun String.unaryMinus() {
        parts += SqlDialectSourcePatternPart.optional(this)
    }

    public fun oneOf(vararg terms: String) {
        parts += SqlDialectSourcePatternPart.oneOf(*terms)
    }

    public fun optionalOneOf(vararg terms: String) {
        parts += SqlDialectSourcePatternPart.optionalOneOf(*terms)
    }

    public fun build(): SqlDialectSourcePatternExpression =
        SqlDialectSourcePatternExpression(parts.toList())
}
