package dev.s7a.sqldelight.check.rule.api

import dev.s7a.sqldelight.check.api.SourceRange

/**
 * Stable facts for a table or derived-table reference.
 */
public class SqlTableReferenceFacts(
    /**
     * Source range covered by the table reference.
     */
    public val range: SourceRange,
    /**
     * Referenced table name when the source is a named table.
     */
    public val name: String? = null,
    /**
     * Alias declared for this table reference.
     */
    public val alias: String? = null,
    /**
     * True when this reference is a derived table or subquery.
     */
    public val subquery: Boolean = false,
) {
    override fun equals(other: Any?): Boolean =
        this === other ||
            other is SqlTableReferenceFacts &&
            range == other.range &&
            name == other.name &&
            alias == other.alias &&
            subquery == other.subquery

    override fun hashCode(): Int {
        var result = range.hashCode()
        result = 31 * result + (name?.hashCode() ?: 0)
        result = 31 * result + (alias?.hashCode() ?: 0)
        result = 31 * result + subquery.hashCode()
        return result
    }

    override fun toString(): String = "SqlTableReferenceFacts(range=$range, name=$name, alias=$alias, subquery=$subquery)"
}
