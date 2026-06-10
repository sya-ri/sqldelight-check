package dev.s7a.sqldelight.check.api

/**
 * Database family for known SQLDelight dialects.
 */
public enum class DialectFamily {
    /** SQLite dialect family, including SQLDelight's versioned SQLite dialect artifacts. */
    SQLite,

    /** MySQL dialect family. */
    MySql,

    /** PostgreSQL dialect family. */
    PostgreSql,

    /** HSQL dialect family. */
    Hsql,

    /** Third-party or otherwise unknown dialect family. */
    Custom,
}
