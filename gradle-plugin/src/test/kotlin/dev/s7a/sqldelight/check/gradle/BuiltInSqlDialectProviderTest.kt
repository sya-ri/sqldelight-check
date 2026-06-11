package dev.s7a.sqldelight.check.gradle

import dev.s7a.sqldelight.check.api.DialectCapabilities
import dev.s7a.sqldelight.check.api.DialectFamily
import dev.s7a.sqldelight.check.api.SqlDialect
import dev.s7a.sqldelight.check.api.SqlDialectCoordinate
import dev.s7a.sqldelight.check.api.SqlDialectSourceKeywords
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests for SQLDelight built-in dialect resolution.
 */
class BuiltInSqlDialectProviderTest {
    @Test
    fun `maps official sqldelight dialect artifacts`() {
        assertEquals(
            SqlDialect(
                family = DialectFamily.SQLite,
                capabilities = setOf(DialectCapabilities.SQLite),
                sourceKeywords = SqlDialectSourceKeywords.SQLite,
            ),
            BuiltInSqlDialectProvider().resolve(
                SqlDialectCoordinate(
                    group = "app.cash.sqldelight",
                    module = "sqlite-3-38-dialect",
                    version = "2.3.2",
                ),
            ),
        )
        assertEquals(
            SqlDialect(
                family = DialectFamily.MySql,
                capabilities = setOf(DialectCapabilities.MySql),
                sourceKeywords = SqlDialectSourceKeywords.MySql,
            ),
            BuiltInSqlDialectProvider().resolve(
                SqlDialectCoordinate(
                    group = "app.cash.sqldelight",
                    module = "mysql-dialect",
                    version = "2.3.2",
                ),
            ),
        )
        assertEquals(
            SqlDialect(
                family = DialectFamily.PostgreSql,
                capabilities = setOf(DialectCapabilities.PostgreSql),
                sourceKeywords = SqlDialectSourceKeywords.PostgreSql,
            ),
            BuiltInSqlDialectProvider().resolve(
                SqlDialectCoordinate(
                    group = "app.cash.sqldelight",
                    module = "postgresql-dialect",
                    version = "2.3.2",
                ),
            ),
        )
        assertEquals(
            SqlDialect(
                family = DialectFamily.Hsql,
                capabilities = setOf(DialectCapabilities.Hsql),
                sourceKeywords = SqlDialectSourceKeywords.Hsql,
            ),
            BuiltInSqlDialectProvider().resolve(
                SqlDialectCoordinate(
                    group = "app.cash.sqldelight",
                    module = "hsql-dialect",
                    version = "2.3.2",
                ),
            ),
        )
    }

    @Test
    fun `ignores third party artifacts`() {
        assertNull(
            BuiltInSqlDialectProvider().resolve(
                SqlDialectCoordinate(
                    group = "com.example",
                    module = "spanner-dialect",
                    version = "1.2.3",
                ),
            ),
        )
    }
}
