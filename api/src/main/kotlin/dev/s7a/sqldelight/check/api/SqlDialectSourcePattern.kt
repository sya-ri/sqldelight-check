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
 * A SQL source term that source-text rules may need to recognize directly.
 */
public sealed interface SqlDialectSourceTerm {
    public val normalizedText: String

    public data object All : SqlDialectSourceTerm {
        override val normalizedText: String = "all"
    }

    public data object And : SqlDialectSourceTerm {
        override val normalizedText: String = "and"
    }

    public data object As : SqlDialectSourceTerm {
        override val normalizedText: String = "as"
    }

    public data object Asc : SqlDialectSourceTerm {
        override val normalizedText: String = "asc"
    }

    public data object Between : SqlDialectSourceTerm {
        override val normalizedText: String = "between"
    }

    public data object By : SqlDialectSourceTerm {
        override val normalizedText: String = "by"
    }

    public data object Case : SqlDialectSourceTerm {
        override val normalizedText: String = "case"
    }

    public data object Count : SqlDialectSourceTerm {
        override val normalizedText: String = "count"
    }

    public data object Create : SqlDialectSourceTerm {
        override val normalizedText: String = "create"
    }

    public data object Delete : SqlDialectSourceTerm {
        override val normalizedText: String = "delete"
    }

    public data object Desc : SqlDialectSourceTerm {
        override val normalizedText: String = "desc"
    }

    public data object Distinct : SqlDialectSourceTerm {
        override val normalizedText: String = "distinct"
    }

    public data object Do : SqlDialectSourceTerm {
        override val normalizedText: String = "do"
    }

    public data object Drop : SqlDialectSourceTerm {
        override val normalizedText: String = "drop"
    }

    public data object Else : SqlDialectSourceTerm {
        override val normalizedText: String = "else"
    }

    public data object End : SqlDialectSourceTerm {
        override val normalizedText: String = "end"
    }

    public data object Escape : SqlDialectSourceTerm {
        override val normalizedText: String = "escape"
    }

    public data object Exists : SqlDialectSourceTerm {
        override val normalizedText: String = "exists"
    }

    public data object False : SqlDialectSourceTerm {
        override val normalizedText: String = "false"
    }

    public data object Fetch : SqlDialectSourceTerm {
        override val normalizedText: String = "fetch"
    }

    public data object Filter : SqlDialectSourceTerm {
        override val normalizedText: String = "filter"
    }

    public data object First : SqlDialectSourceTerm {
        override val normalizedText: String = "first"
    }

    public data object From : SqlDialectSourceTerm {
        override val normalizedText: String = "from"
    }

    public data object Group : SqlDialectSourceTerm {
        override val normalizedText: String = "group"
    }

    public data object Having : SqlDialectSourceTerm {
        override val normalizedText: String = "having"
    }

    public data object In : SqlDialectSourceTerm {
        override val normalizedText: String = "in"
    }

    public data object Insert : SqlDialectSourceTerm {
        override val normalizedText: String = "insert"
    }

    public data object Into : SqlDialectSourceTerm {
        override val normalizedText: String = "into"
    }

    public data object Is : SqlDialectSourceTerm {
        override val normalizedText: String = "is"
    }

    public data object Join : SqlDialectSourceTerm {
        override val normalizedText: String = "join"
    }

    public data object Last : SqlDialectSourceTerm {
        override val normalizedText: String = "last"
    }

    public data object Like : SqlDialectSourceTerm {
        override val normalizedText: String = "like"
    }

    public data object Limit : SqlDialectSourceTerm {
        override val normalizedText: String = "limit"
    }

    public data object Not : SqlDialectSourceTerm {
        override val normalizedText: String = "not"
    }

    public data object Null : SqlDialectSourceTerm {
        override val normalizedText: String = "null"
    }

    public data object Nulls : SqlDialectSourceTerm {
        override val normalizedText: String = "nulls"
    }

    public data object Offset : SqlDialectSourceTerm {
        override val normalizedText: String = "offset"
    }

    public data object On : SqlDialectSourceTerm {
        override val normalizedText: String = "on"
    }

    public data object Or : SqlDialectSourceTerm {
        override val normalizedText: String = "or"
    }

    public data object Order : SqlDialectSourceTerm {
        override val normalizedText: String = "order"
    }

    public data object Over : SqlDialectSourceTerm {
        override val normalizedText: String = "over"
    }

    public data object Outer : SqlDialectSourceTerm {
        override val normalizedText: String = "outer"
    }

    public data object Right : SqlDialectSourceTerm {
        override val normalizedText: String = "right"
    }

    public data object Rollback : SqlDialectSourceTerm {
        override val normalizedText: String = "rollback"
    }

    public data object Select : SqlDialectSourceTerm {
        override val normalizedText: String = "select"
    }

    public data object Set : SqlDialectSourceTerm {
        override val normalizedText: String = "set"
    }

    public data object Table : SqlDialectSourceTerm {
        override val normalizedText: String = "table"
    }

    public data object Then : SqlDialectSourceTerm {
        override val normalizedText: String = "then"
    }

    public data object Transaction : SqlDialectSourceTerm {
        override val normalizedText: String = "transaction"
    }

    public data object Trigger : SqlDialectSourceTerm {
        override val normalizedText: String = "trigger"
    }

    public data object True : SqlDialectSourceTerm {
        override val normalizedText: String = "true"
    }

    public data object Union : SqlDialectSourceTerm {
        override val normalizedText: String = "union"
    }

    public data object Update : SqlDialectSourceTerm {
        override val normalizedText: String = "update"
    }

    public data object Using : SqlDialectSourceTerm {
        override val normalizedText: String = "using"
    }

    public data object Values : SqlDialectSourceTerm {
        override val normalizedText: String = "values"
    }

    public data object View : SqlDialectSourceTerm {
        override val normalizedText: String = "view"
    }

    public data object When : SqlDialectSourceTerm {
        override val normalizedText: String = "when"
    }

    public data object Where : SqlDialectSourceTerm {
        override val normalizedText: String = "where"
    }

    public data object Window : SqlDialectSourceTerm {
        override val normalizedText: String = "window"
    }

    public data object With : SqlDialectSourceTerm {
        override val normalizedText: String = "with"
    }
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
