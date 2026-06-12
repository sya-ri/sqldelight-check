package dev.s7a.sqldelight.check.dialects.postgres

import dev.s7a.sqldelight.check.api.DialectCapability
import dev.s7a.sqldelight.check.api.DialectFamily
import dev.s7a.sqldelight.check.api.SqlDialect

/**
 * PostgreSQL dialect family.
 */
public val PostgresDialectFamily: DialectFamily = DialectFamily.Named("postgresql")

/**
 * PostgreSQL-compatible syntax and behavior.
 */
public val PostgresDialectCapability: DialectCapability = DialectCapability("postgresql")

/**
 * SQLDelight PostgreSQL dialect metadata.
 */
public val PostgresDialect: SqlDialect =
    SqlDialect(
        family = PostgresDialectFamily,
        capabilities = setOf(PostgresDialectCapability),
        sourcePatterns = PostgresDialectSourcePatterns,
    )
