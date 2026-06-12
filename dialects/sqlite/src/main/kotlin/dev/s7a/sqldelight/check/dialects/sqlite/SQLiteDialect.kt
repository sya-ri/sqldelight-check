package dev.s7a.sqldelight.check.dialects.sqlite

import dev.s7a.sqldelight.check.api.DialectCapability
import dev.s7a.sqldelight.check.api.DialectFamily
import dev.s7a.sqldelight.check.api.SqlDialect

/**
 * SQLite dialect family.
 */
public val SQLiteDialectFamily: DialectFamily = DialectFamily.Named("sqlite")

/**
 * SQLite-compatible syntax and behavior.
 */
public val SQLiteDialectCapability: DialectCapability = DialectCapability("sqlite")

/**
 * SQLDelight SQLite dialect metadata.
 */
public val SQLiteDialect: SqlDialect =
    SqlDialect(
        family = SQLiteDialectFamily,
        capabilities = setOf(SQLiteDialectCapability),
        sourcePatterns = SQLiteDialectSourcePatterns,
    )
