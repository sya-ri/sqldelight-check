package dev.s7a.sqldelight.check.dialects.postgres

import dev.s7a.sqldelight.check.api.DialectId
import dev.s7a.sqldelight.check.api.SqlDialect

/**
 * PostgreSQL dialect ID.
 */
public val PostgresDialectId: DialectId = DialectId("postgresql")

/**
 * SQLDelight PostgreSQL dialect metadata.
 */
public val PostgresDialect: SqlDialect =
    SqlDialect(
        ids = setOf(PostgresDialectId),
        sourcePatterns = PostgresDialectSourcePatterns,
    )
