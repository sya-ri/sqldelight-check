package dev.s7a.sqldelight.check.dialects.sqldelight

import dev.s7a.sqldelight.check.api.DialectCapability
import dev.s7a.sqldelight.check.api.DialectFamily
import dev.s7a.sqldelight.check.api.SqlDialect
import dev.s7a.sqldelight.check.api.SqlDialectCoordinate
import dev.s7a.sqldelight.check.api.SqlDialectProvider
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatterns

private const val SQLDELIGHT_GROUP = "app.cash.sqldelight"
private const val DIALECT_SUFFIX = "-dialect"

/**
 * Resolves SQLDelight's built-in dialect artifacts.
 */
public class SqlDelightDialectProvider : SqlDialectProvider {
    override fun resolve(coordinate: SqlDialectCoordinate): SqlDialect? {
        if (coordinate.group != SQLDELIGHT_GROUP) return null
        return when (coordinate.module) {
            "mysql-dialect" -> MySqlDialect
            "postgresql-dialect" -> PostgreSqlDialect
            "hsql-dialect" -> HsqlDialect
            else ->
                if (coordinate.module.startsWith("sqlite-") && coordinate.module.endsWith(DIALECT_SUFFIX)) {
                    SQLiteDialect
                } else {
                    null
                }
        }
    }
}

private val SQLiteDialect: SqlDialect =
    SqlDialect(
        family = DialectFamily.SQLite,
        capabilities = setOf(DialectCapability.SQLite),
        sourcePatterns = SqlDialectSourcePatterns.SQLite,
    )

private val MySqlDialect: SqlDialect =
    SqlDialect(
        family = DialectFamily.MySql,
        capabilities = setOf(DialectCapability.MySql),
        sourcePatterns = SqlDialectSourcePatterns.MySql,
    )

private val PostgreSqlDialect: SqlDialect =
    SqlDialect(
        family = DialectFamily.PostgreSql,
        capabilities = setOf(DialectCapability.PostgreSql),
        sourcePatterns = SqlDialectSourcePatterns.PostgreSql,
    )

private val HsqlDialect: SqlDialect =
    SqlDialect(
        family = DialectFamily.Hsql,
        capabilities = setOf(DialectCapability.Hsql),
        sourcePatterns = SqlDialectSourcePatterns.Hsql,
    )
