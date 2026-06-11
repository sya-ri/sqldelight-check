package dev.s7a.sqldelight.check.api

/**
 * Dialect-specific keyword groups used by the conservative source scanner.
 *
 * The scanner uses these groups only to split and label source-level SQL facts;
 * they are not a full parser grammar. Custom dialect integrations can extend
 * the source scanner default or replace the keyword groups when their syntax
 * has different clause boundaries or join modifiers.
 */
public class SqlDialectSourceKeywords(
    /**
     * Keywords that should not be treated as implicit SELECT result aliases.
     */
    public val aliasBoundaryKeywords: Set<String> = SourceScannerDefault.aliasBoundaryKeywords,
    /**
     * Keywords that end a table-reference segment.
     */
    public val tableReferenceBoundaryKeywords: Set<String> = SourceScannerDefault.tableReferenceBoundaryKeywords,
    /**
     * Keywords that can appear directly before JOIN as join-kind modifiers.
     */
    public val joinModifierKeywords: Set<String> = SourceScannerDefault.joinModifierKeywords,
) {
    /**
     * Returns a copy with dialect-specific keyword additions and removals applied.
     */
    public fun extend(
        addAliasBoundaryKeywords: Set<String> = emptySet(),
        removeAliasBoundaryKeywords: Set<String> = emptySet(),
        addTableReferenceBoundaryKeywords: Set<String> = emptySet(),
        removeTableReferenceBoundaryKeywords: Set<String> = emptySet(),
        addJoinModifierKeywords: Set<String> = emptySet(),
        removeJoinModifierKeywords: Set<String> = emptySet(),
    ): SqlDialectSourceKeywords =
        SqlDialectSourceKeywords(
            aliasBoundaryKeywords = aliasBoundaryKeywords.extendedWith(addAliasBoundaryKeywords, removeAliasBoundaryKeywords),
            tableReferenceBoundaryKeywords =
                tableReferenceBoundaryKeywords.extendedWith(
                    addTableReferenceBoundaryKeywords,
                    removeTableReferenceBoundaryKeywords,
                ),
            joinModifierKeywords = joinModifierKeywords.extendedWith(addJoinModifierKeywords, removeJoinModifierKeywords),
        )

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
         * Conservative fallback used by the source scanner when no known dialect preset applies.
         */
        public val SourceScannerDefault: SqlDialectSourceKeywords =
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

        /**
         * SQLite source scanner keywords.
         */
        public val SQLite: SqlDialectSourceKeywords =
            SourceScannerDefault.extend(
                removeTableReferenceBoundaryKeywords = setOf("full", "right"),
                removeJoinModifierKeywords = setOf("full", "right"),
            )

        /**
         * MySQL source scanner keywords.
         */
        public val MySql: SqlDialectSourceKeywords =
            SourceScannerDefault.extend(
                addTableReferenceBoundaryKeywords = setOf("for"),
            )

        /**
         * PostgreSQL source scanner keywords.
         */
        public val PostgreSql: SqlDialectSourceKeywords =
            SourceScannerDefault.extend(
                addTableReferenceBoundaryKeywords = setOf("fetch", "for"),
            )

        /**
         * HSQL source scanner keywords.
         */
        public val Hsql: SqlDialectSourceKeywords =
            SourceScannerDefault.extend(
                addTableReferenceBoundaryKeywords = setOf("fetch"),
            )
    }
}

private fun Set<String>.extendedWith(
    additions: Set<String>,
    removals: Set<String>,
): Set<String> =
    buildSet(size + additions.size) {
        addAll(this@extendedWith)
        removeAll(removals)
        addAll(additions)
    }
