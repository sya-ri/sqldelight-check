package dev.s7a.sqldelight.check.api

/**
 * Built-in dialect capabilities inferred for SQLDelight's official dialect artifacts.
 */
public object DialectCapabilities {
    /** SQLite-compatible syntax and behavior. */
    public val SQLite: DialectCapability = DialectCapability("sqlite")

    /** MySQL-compatible syntax and behavior. */
    public val MySql: DialectCapability = DialectCapability("mysql")

    /** PostgreSQL-compatible syntax and behavior. */
    public val PostgreSql: DialectCapability = DialectCapability("postgresql")

    /** HSQL-compatible syntax and behavior. */
    public val Hsql: DialectCapability = DialectCapability("hsql")
}
