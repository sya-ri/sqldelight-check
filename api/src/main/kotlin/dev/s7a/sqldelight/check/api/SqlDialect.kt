package dev.s7a.sqldelight.check.api

/**
 * Metadata describing the SQL dialect used by a SQLDelight database.
 */
public class SqlDialect(
    /**
     * Broad dialect family used by rule applicability checks.
     */
    public val family: DialectFamily,
    /**
     * Human-readable dialect name for reports.
     */
    public val displayName: String,
    /**
     * Maven coordinate for the dialect artifact when known.
     */
    public val artifact: String? = null,
    /**
     * Dialect artifact version when known.
     */
    public val version: String? = null,
    /**
     * Implementation class name for custom dialects when available.
     */
    public val implementationClass: String? = null,
    /**
     * Capabilities discovered or inferred for this dialect.
     */
    public val capabilities: Set<DialectCapability> = emptySet(),
    /**
     * Keyword groups used by source-level SQL fact extraction.
     */
    public val sourceKeywords: SqlDialectSourceKeywords = SqlDialectSourceKeywords.Default,
) {
    override fun equals(other: Any?): Boolean =
        this === other ||
            other is SqlDialect &&
            family == other.family &&
            displayName == other.displayName &&
            artifact == other.artifact &&
            version == other.version &&
            implementationClass == other.implementationClass &&
            capabilities == other.capabilities &&
            sourceKeywords == other.sourceKeywords

    override fun hashCode(): Int {
        var result = family.hashCode()
        result = 31 * result + displayName.hashCode()
        result = 31 * result + (artifact?.hashCode() ?: 0)
        result = 31 * result + (version?.hashCode() ?: 0)
        result = 31 * result + (implementationClass?.hashCode() ?: 0)
        result = 31 * result + capabilities.hashCode()
        result = 31 * result + sourceKeywords.hashCode()
        return result
    }

    override fun toString(): String =
        "SqlDialect(family=$family, displayName=$displayName, artifact=$artifact, version=$version, implementationClass=$implementationClass, capabilities=$capabilities, sourceKeywords=$sourceKeywords)"
}
