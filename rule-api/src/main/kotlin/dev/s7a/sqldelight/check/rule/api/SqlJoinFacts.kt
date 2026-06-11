package dev.s7a.sqldelight.check.rule.api

import dev.s7a.sqldelight.check.api.SourceRange

/**
 * Stable facts for a JOIN clause.
 */
public class SqlJoinFacts(
    /**
     * Source range covered by the JOIN keyword and joined source.
     */
    public val range: SourceRange,
    /**
     * Join kind text as it appears before the JOIN keyword.
     */
    public val kind: String,
    /**
     * Joined table or derived-table reference.
     */
    public val table: SqlTableReferenceFacts,
) {
    override fun equals(other: Any?): Boolean =
        this === other ||
            other is SqlJoinFacts &&
            range == other.range &&
            kind == other.kind &&
            table == other.table

    override fun hashCode(): Int {
        var result = range.hashCode()
        result = 31 * result + kind.hashCode()
        result = 31 * result + table.hashCode()
        return result
    }

    override fun toString(): String = "SqlJoinFacts(range=$range, kind=$kind, table=$table)"
}
