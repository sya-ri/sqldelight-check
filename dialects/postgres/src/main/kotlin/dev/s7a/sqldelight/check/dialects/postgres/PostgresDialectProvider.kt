package dev.s7a.sqldelight.check.dialects.postgres

import dev.s7a.sqldelight.check.api.SqlDialect
import dev.s7a.sqldelight.check.api.SqlDialectCoordinate
import dev.s7a.sqldelight.check.api.SqlDialectProvider

private const val SQLDELIGHT_GROUP = "app.cash.sqldelight"

/**
 * Resolves SQLDelight's PostgreSQL dialect artifact.
 */
public class PostgresDialectProvider : SqlDialectProvider {
    override fun resolve(coordinate: SqlDialectCoordinate): SqlDialect? {
        if (coordinate.group != SQLDELIGHT_GROUP) return null
        if (coordinate.module != "postgresql-dialect") return null
        return PostgresDialect
    }
}
