package dev.s7a.sqldelight.check.rule.api

import dev.s7a.sqldelight.check.api.SourceRange

/**
 * Stable facts for one top-level SQL statement.
 */
public class SqlStatementFacts(
    /**
     * Statement kind inferred from the leading SQL keyword.
     */
    public val kind: SqlStatementKind,
    /**
     * Source range covered by this statement.
     */
    public val range: SourceRange,
    /**
     * SELECT-specific facts when this statement is a SELECT.
     */
    public val select: SqlSelectFacts? = null,
    /**
     * Table references visible at the statement's top level.
     */
    public val tableReferences: List<SqlTableReferenceFacts> = emptyList(),
    /**
     * JOIN clauses visible at the statement's top level.
     */
    public val joins: List<SqlJoinFacts> = emptyList(),
    /**
     * Qualified column-like references discovered in the statement.
     */
    public val qualifiedReferences: List<SqlQualifiedReferenceFacts> = emptyList(),
) {
    override fun equals(other: Any?): Boolean =
        this === other ||
            other is SqlStatementFacts &&
            kind == other.kind &&
            range == other.range &&
            select == other.select &&
            tableReferences == other.tableReferences &&
            joins == other.joins &&
            qualifiedReferences == other.qualifiedReferences

    override fun hashCode(): Int {
        var result = kind.hashCode()
        result = 31 * result + range.hashCode()
        result = 31 * result + (select?.hashCode() ?: 0)
        result = 31 * result + tableReferences.hashCode()
        result = 31 * result + joins.hashCode()
        result = 31 * result + qualifiedReferences.hashCode()
        return result
    }

    override fun toString(): String =
        "SqlStatementFacts(kind=$kind, range=$range, select=$select, tableReferences=$tableReferences, joins=$joins, qualifiedReferences=$qualifiedReferences)"
}
