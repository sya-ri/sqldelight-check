package dev.s7a.sqldelight.check.api

/**
 * Rule-relevant SQL dialect metadata for a SQLDelight database.
 */
public class SqlDialect(
    /**
     * Broad dialect family used by rule applicability checks.
     */
    public val family: DialectFamily,
    /**
     * Capabilities discovered or inferred for this dialect.
     */
    public val capabilities: Set<DialectCapability> = emptySet(),
    /**
     * Keyword groups used by source-level SQL fact extraction.
     */
    public val sourceKeywords: SqlDialectSourceKeywords = SqlDialectSourceKeywords.SourceScannerDefault,
) {
    override fun equals(other: Any?): Boolean =
        this === other ||
            other is SqlDialect &&
            family == other.family &&
            capabilities == other.capabilities &&
            sourceKeywords == other.sourceKeywords

    override fun hashCode(): Int {
        var result = family.hashCode()
        result = 31 * result + capabilities.hashCode()
        result = 31 * result + sourceKeywords.hashCode()
        return result
    }

    override fun toString(): String =
        "SqlDialect(family=$family, capabilities=$capabilities, sourceKeywords=$sourceKeywords)"
}
