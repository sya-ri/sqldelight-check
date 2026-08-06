package dev.s7a.sqldelight.check.api

/**
 * A dialect source pattern expression such as `ORDER BY`, `LEFT [OUTER] JOIN`,
 * `FETCH {FIRST|NEXT}`, or `FETCH {FIRST|NEXT} [ROW|ROWS]`.
 */
public class SqlDialectSourcePatternExpression(
    public val parts: List<SqlDialectSourcePatternPart>,
) {
    /**
     * Returns the number of input terms consumed when [terms] match this expression from the start.
     */
    public fun matchPrefix(terms: List<String>): Int? {
        var inputIndex = 0
        parts.forEach { part ->
            val input = terms.getOrNull(inputIndex)?.lowercase()
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
        /**
         * Parses a compact source pattern expression.
         *
         * Supported forms:
         *
         * - `ORDER BY`
         * - `LEFT [OUTER] JOIN`
         * - `FETCH {FIRST|NEXT} [ROW|ROWS]`
         */
        public fun parse(expression: String): SqlDialectSourcePatternExpression =
            SqlDialectSourcePatternExpression(expression.patternParts())
    }
}
