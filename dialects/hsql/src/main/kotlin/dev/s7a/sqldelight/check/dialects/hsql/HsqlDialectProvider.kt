package dev.s7a.sqldelight.check.dialects.hsql

import dev.s7a.sqldelight.check.api.SqlDialect
import dev.s7a.sqldelight.check.api.SqlDialectCoordinate
import dev.s7a.sqldelight.check.api.SqlDialectProvider

private const val SQLDELIGHT_GROUP = "app.cash.sqldelight"

/**
 * Resolves SQLDelight's HSQL dialect artifact.
 */
public class HsqlDialectProvider : SqlDialectProvider {
    override fun resolve(coordinate: SqlDialectCoordinate): SqlDialect? {
        if (coordinate.group != SQLDELIGHT_GROUP) return null
        if (coordinate.module != "hsql-dialect") return null
        return HsqlDialect
    }
}
