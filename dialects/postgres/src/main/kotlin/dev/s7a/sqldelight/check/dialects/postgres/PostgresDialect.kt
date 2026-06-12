package dev.s7a.sqldelight.check.dialects.postgres

import dev.s7a.sqldelight.check.api.DialectCapability
import dev.s7a.sqldelight.check.api.DialectFamily
import dev.s7a.sqldelight.check.api.SqlDialect

/**
 * SQLDelight PostgreSQL dialect metadata.
 */
public val PostgresDialect: SqlDialect =
    SqlDialect(
        family = DialectFamily.PostgreSql,
        capabilities = setOf(DialectCapability.PostgreSql),
        sourcePatterns = PostgresDialectSourcePatterns,
    )
