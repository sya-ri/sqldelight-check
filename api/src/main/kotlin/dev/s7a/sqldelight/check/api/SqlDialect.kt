package dev.s7a.sqldelight.check.api

/**
 * Rule-relevant SQL dialect metadata for a SQLDelight database.
 */
public class SqlDialect(
    /**
     * Dialect IDs discovered or inferred for this dialect.
     */
    public val ids: Set<DialectId> = setOf(DialectId.Unknown),
    /**
     * Source patterns used by source-level SQL fact extraction.
     */
    public val sourcePatterns: SqlDialectSourcePatterns = SqlDialectSourcePatterns.SourceScannerDefault,
) {
    override fun equals(other: Any?): Boolean =
        this === other ||
            other is SqlDialect &&
            ids == other.ids &&
            sourcePatterns == other.sourcePatterns

    override fun hashCode(): Int {
        var result = ids.hashCode()
        result = 31 * result + sourcePatterns.hashCode()
        return result
    }

    override fun toString(): String =
        "SqlDialect(ids=$ids, sourcePatterns=$sourcePatterns)"
}
