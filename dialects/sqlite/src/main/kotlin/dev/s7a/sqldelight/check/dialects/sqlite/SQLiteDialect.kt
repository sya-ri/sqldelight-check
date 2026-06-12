package dev.s7a.sqldelight.check.dialects.sqlite

import dev.s7a.sqldelight.check.api.DialectCapability
import dev.s7a.sqldelight.check.api.DialectFamily
import dev.s7a.sqldelight.check.api.SqlDialect

/**
 * SQLDelight SQLite dialect metadata.
 */
public val SQLiteDialect: SqlDialect =
    SqlDialect(
        family = DialectFamily.SQLite,
        capabilities = setOf(DialectCapability.SQLite),
        sourcePatterns = SQLiteDialectSourcePatterns,
    )
