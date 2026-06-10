package dev.s7a.sqldelight.check.api

/**
 * Lightweight SQLDelight database context visible to rules and reporters.
 */
public data class DatabaseContext(
    /**
     * SQLDelight database name.
     */
    public val name: String,
    /**
     * Dialect used to analyze files in this database.
     */
    public val dialect: SqlDialect,
)
