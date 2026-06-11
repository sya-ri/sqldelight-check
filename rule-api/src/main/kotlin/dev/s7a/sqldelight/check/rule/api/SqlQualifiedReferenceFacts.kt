package dev.s7a.sqldelight.check.rule.api

import dev.s7a.sqldelight.check.api.SourceRange

/**
 * Stable facts for a qualified reference such as `table.column`.
 *
 * The model records source syntax only. It does not claim that the qualifier
 * resolves to a table in scope or that the name resolves to a real column.
 */
public class SqlQualifiedReferenceFacts(
    /**
     * Source range covered by the whole qualified reference.
     */
    public val range: SourceRange,
    /**
     * Qualifier text before the dot.
     */
    public val qualifier: String,
    /**
     * Referenced name after the dot.
     */
    public val name: String,
) {
    override fun equals(other: Any?): Boolean =
        this === other ||
            other is SqlQualifiedReferenceFacts &&
            range == other.range &&
            qualifier == other.qualifier &&
            name == other.name

    override fun hashCode(): Int {
        var result = range.hashCode()
        result = 31 * result + qualifier.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }

    override fun toString(): String = "SqlQualifiedReferenceFacts(range=$range, qualifier=$qualifier, name=$name)"
}
