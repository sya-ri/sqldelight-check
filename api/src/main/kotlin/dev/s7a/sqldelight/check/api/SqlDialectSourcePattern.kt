package dev.s7a.sqldelight.check.api

/**
 * One part in a dialect source pattern expression.
 *
 * A part can match any value in [alternatives]. Optional parts can be omitted
 * when matching the whole expression.
 */
public class SqlDialectSourcePatternPart(
    alternatives: Set<String>,
    public val optional: Boolean = false,
) {
    public val alternatives: Set<String> = alternatives.normalizedSqlTerms()

    override fun equals(other: Any?): Boolean =
        this === other ||
            other is SqlDialectSourcePatternPart &&
            alternatives == other.alternatives &&
            optional == other.optional

    override fun hashCode(): Int = 31 * alternatives.hashCode() + optional.hashCode()

    override fun toString(): String =
        "SqlDialectSourcePatternPart(alternatives=$alternatives, optional=$optional)"

    public companion object {
        public fun required(term: String): SqlDialectSourcePatternPart =
            SqlDialectSourcePatternPart(setOf(term))

        public fun oneOf(vararg terms: String): SqlDialectSourcePatternPart =
            SqlDialectSourcePatternPart(terms.toSet())

        public fun optional(term: String): SqlDialectSourcePatternPart =
            SqlDialectSourcePatternPart(setOf(term), optional = true)

        public fun optionalOneOf(vararg terms: String): SqlDialectSourcePatternPart =
            SqlDialectSourcePatternPart(terms.toSet(), optional = true)
    }
}

/**
 * A dialect source pattern expression such as `ORDER BY`, `LEFT [OUTER] JOIN`,
 * or `FETCH {FIRST|NEXT}`.
 */
public class SqlDialectSourcePatternExpression(
    parts: List<SqlDialectSourcePatternPart>,
) {
    public val parts: List<SqlDialectSourcePatternPart> = parts

    /**
     * Returns the number of input terms consumed when [terms] match this expression from the start.
     */
    public fun matchPrefix(terms: List<String>): Int? {
        val normalized = terms.map { term -> term.lowercase() }
        var inputIndex = 0
        parts.forEach { part ->
            val input = normalized.getOrNull(inputIndex)
            if (input in part.alternatives) {
                inputIndex++
            } else if (!part.optional) {
                return null
            }
        }
        return inputIndex
    }

    override fun equals(other: Any?): Boolean =
        this === other ||
            other is SqlDialectSourcePatternExpression &&
            parts == other.parts

    override fun hashCode(): Int = parts.hashCode()

    override fun toString(): String =
        "SqlDialectSourcePatternExpression(parts=$parts)"

    public companion object {
        public fun terms(vararg terms: String): SqlDialectSourcePatternExpression =
            SqlDialectSourcePatternExpression(terms.map { term -> SqlDialectSourcePatternPart.required(term) })

        public fun sequence(vararg parts: SqlDialectSourcePatternPart): SqlDialectSourcePatternExpression =
            SqlDialectSourcePatternExpression(parts.toList())

        public fun build(block: SqlDialectSourcePatternExpressionBuilder.() -> Unit): SqlDialectSourcePatternExpression =
            SqlDialectSourcePatternExpressionBuilder().apply(block).build()

        /**
         * Parses a compact source pattern expression.
         *
         * Supported forms:
         *
         * - `ORDER BY`
         * - `LEFT [OUTER] JOIN`
         * - `FETCH {FIRST|NEXT} [ROW]`
         */
        public fun parse(expression: String): SqlDialectSourcePatternExpression =
            SqlDialectSourcePatternExpression(expression.patternParts())
    }
}

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

/**
 * The source-scanner meaning attached to a dialect pattern.
 */
public sealed interface SqlDialectSourcePatternRole {
    public data object AliasBoundary : SqlDialectSourcePatternRole

    public data object TableReferenceBoundary : SqlDialectSourcePatternRole

    public data object JoinModifier : SqlDialectSourcePatternRole

    public data object StatementStart : SqlDialectSourcePatternRole

    public data object SqlDelightStatementStart : SqlDialectSourcePatternRole

    public data object StatementContinuation : SqlDialectSourcePatternRole

    public data object SelectListStart : SqlDialectSourcePatternRole

    public data object ClauseBoundary : SqlDialectSourcePatternRole

    public data object ExpressionContinuation : SqlDialectSourcePatternRole

    public data object ParenthesizedExpressionContinuation : SqlDialectSourcePatternRole
}

/**
 * A dialect source pattern and the scanner roles it fulfills.
 */
public class SqlDialectSourcePattern(
    public val expression: SqlDialectSourcePatternExpression,
    roles: Set<SqlDialectSourcePatternRole>,
) {
    public val roles: Set<SqlDialectSourcePatternRole> = roles

    override fun equals(other: Any?): Boolean =
        this === other ||
            other is SqlDialectSourcePattern &&
            expression == other.expression &&
            roles == other.roles

    override fun hashCode(): Int = 31 * expression.hashCode() + roles.hashCode()

    override fun toString(): String =
        "SqlDialectSourcePattern(expression=$expression, roles=$roles)"

    public companion object {
        public fun parse(
            expression: String,
            vararg roles: SqlDialectSourcePatternRole,
        ): SqlDialectSourcePattern =
            SqlDialectSourcePattern(
                expression = SqlDialectSourcePatternExpression.parse(expression),
                roles = roles.toSet(),
            )
    }
}

internal fun Set<String>.normalizedSqlTerms(): Set<String> =
    mapTo(mutableSetOf()) { term -> term.lowercase() }

private fun String.patternParts(): List<SqlDialectSourcePatternPart> =
    trim()
        .split(Regex("\\s+"))
        .filter { token -> token.isNotBlank() }
        .map { token -> token.patternPart() }

private fun String.patternPart(): SqlDialectSourcePatternPart {
    val optional = startsWith("[") && endsWith("]")
    val choice = startsWith("{") && endsWith("}")
    val body =
        when {
            optional || choice -> substring(1, length - 1)
            else -> this
        }
    val alternatives = body.split("|").filter { term -> term.isNotBlank() }.toSet()
    return SqlDialectSourcePatternPart(
        alternatives = alternatives,
        optional = optional,
    )
}
