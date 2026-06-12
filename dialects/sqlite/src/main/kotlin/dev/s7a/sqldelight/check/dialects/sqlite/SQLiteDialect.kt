package dev.s7a.sqldelight.check.dialects.sqlite

import dev.s7a.sqldelight.check.api.DialectId
import dev.s7a.sqldelight.check.api.SqlDialect

/**
 * SQLite dialect ID.
 */
public val SQLiteDialectId: DialectId = DialectId("sqlite")

/**
 * SQLDelight SQLite dialect metadata.
 */
public val SQLiteDialect: SqlDialect =
    SqlDialect(
        ids = setOf(SQLiteDialectId),
        sourcePatterns = SQLiteDialectSourcePatterns,
    )
