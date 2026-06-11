package dev.s7a.sqldelight.check.api

/**
 * Dialect-specific keyword groups used by the conservative source scanner.
 *
 * The scanner uses these groups only to split and label source-level SQL facts;
 * they are not a full parser grammar. Custom dialect integrations can extend or
 * replace the defaults when their syntax has different clause boundaries or
 * join modifiers.
 */
public class SqlDialectSourceKeywords(
    /**
     * Keywords that should not be treated as implicit SELECT result aliases.
     */
    public val aliasBoundaryKeywords: Set<String> = Default.aliasBoundaryKeywords,
    /**
     * Keywords that end a table-reference segment.
     */
    public val tableReferenceBoundaryKeywords: Set<String> = Default.tableReferenceBoundaryKeywords,
    /**
     * Keywords that can appear directly before JOIN as join-kind modifiers.
     */
    public val joinModifierKeywords: Set<String> = Default.joinModifierKeywords,
) {
    override fun equals(other: Any?): Boolean =
        this === other ||
            other is SqlDialectSourceKeywords &&
            aliasBoundaryKeywords == other.aliasBoundaryKeywords &&
            tableReferenceBoundaryKeywords == other.tableReferenceBoundaryKeywords &&
            joinModifierKeywords == other.joinModifierKeywords

    override fun hashCode(): Int {
        var result = aliasBoundaryKeywords.hashCode()
        result = 31 * result + tableReferenceBoundaryKeywords.hashCode()
        result = 31 * result + joinModifierKeywords.hashCode()
        return result
    }

    override fun toString(): String =
        "SqlDialectSourceKeywords(aliasBoundaryKeywords=$aliasBoundaryKeywords, tableReferenceBoundaryKeywords=$tableReferenceBoundaryKeywords, joinModifierKeywords=$joinModifierKeywords)"

    public companion object {
        /**
         * Dialect-neutral defaults used when no custom keyword groups are supplied.
         */
        public val Default: SqlDialectSourceKeywords =
            SqlDialectSourceKeywords(
                aliasBoundaryKeywords =
                    setOf(
                        "case",
                        "cast",
                        "coalesce",
                        "count",
                        "else",
                        "end",
                        "false",
                        "from",
                        "null",
                        "then",
                        "true",
                        "when",
                    ),
                tableReferenceBoundaryKeywords =
                    setOf(
                        "cross",
                        "except",
                        "full",
                        "group",
                        "having",
                        "inner",
                        "intersect",
                        "join",
                        "left",
                        "limit",
                        "offset",
                        "on",
                        "order",
                        "right",
                        "union",
                        "using",
                        "where",
                    ),
                joinModifierKeywords =
                    setOf(
                        "cross",
                        "full",
                        "inner",
                        "join",
                        "left",
                        "outer",
                        "right",
                    ),
            )
    }
}
