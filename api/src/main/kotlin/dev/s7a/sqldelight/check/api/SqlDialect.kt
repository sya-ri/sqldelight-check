package dev.s7a.sqldelight.check.api

/**
 * Metadata describing the SQL dialect used by a SQLDelight database.
 */
public data class SqlDialect(
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
)
