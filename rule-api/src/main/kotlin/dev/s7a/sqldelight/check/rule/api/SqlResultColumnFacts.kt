package dev.s7a.sqldelight.check.rule.api

import dev.s7a.sqldelight.check.api.SourceRange

/**
 * Stable facts for a SELECT result column.
 */
public class SqlResultColumnFacts(
    /**
     * Source range covered by the result column expression and optional alias.
     */
    public val range: SourceRange,
    /**
     * Alias text when the result column declares one.
     */
    public val alias: String? = null,
    /**
     * True when the result column is a wildcard such as `*` or `table.*`.
     */
    public val wildcard: Boolean = false,
) {
    override fun equals(other: Any?): Boolean =
        this === other ||
            other is SqlResultColumnFacts &&
            range == other.range &&
            alias == other.alias &&
            wildcard == other.wildcard

    override fun hashCode(): Int {
        var result = range.hashCode()
        result = 31 * result + (alias?.hashCode() ?: 0)
        result = 31 * result + wildcard.hashCode()
        return result
    }

    override fun toString(): String = "SqlResultColumnFacts(range=$range, alias=$alias, wildcard=$wildcard)"
}
