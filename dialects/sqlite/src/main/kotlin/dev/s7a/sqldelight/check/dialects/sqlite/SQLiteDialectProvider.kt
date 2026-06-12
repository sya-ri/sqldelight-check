package dev.s7a.sqldelight.check.dialects.sqlite

import dev.s7a.sqldelight.check.api.SqlDialect
import dev.s7a.sqldelight.check.api.SqlDialectCoordinate
import dev.s7a.sqldelight.check.api.SqlDialectProvider

private const val SQLDELIGHT_GROUP = "app.cash.sqldelight"
private val sqliteDialectModulePattern = Regex("""sqlite-.+-dialect""")

/**
 * Resolves SQLDelight's SQLite dialect artifacts.
 */
public class SQLiteDialectProvider : SqlDialectProvider {
    override fun resolve(coordinate: SqlDialectCoordinate): SqlDialect? {
        if (coordinate.group != SQLDELIGHT_GROUP) return null
        if (!sqliteDialectModulePattern.matches(coordinate.module)) return null
        return SQLiteDialect
    }
}
