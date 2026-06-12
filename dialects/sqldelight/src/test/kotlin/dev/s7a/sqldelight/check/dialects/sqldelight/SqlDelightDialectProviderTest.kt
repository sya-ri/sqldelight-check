package dev.s7a.sqldelight.check.dialects.sqldelight

import dev.s7a.sqldelight.check.api.DialectCapability
import dev.s7a.sqldelight.check.api.DialectFamily
import dev.s7a.sqldelight.check.api.SqlDialect
import dev.s7a.sqldelight.check.api.SqlDialectCoordinate
import dev.s7a.sqldelight.check.api.SqlDialectProvider
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatterns
import java.util.ServiceLoader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Tests for SQLDelight built-in dialect resolution.
 */
class SqlDelightDialectProviderTest {
    @Test
    fun `maps official sqldelight dialect artifacts`() {
        assertEquals(
            SqlDialect(
                family = DialectFamily.SQLite,
                capabilities = setOf(DialectCapability.SQLite),
                sourcePatterns = SqlDialectSourcePatterns.SQLite,
            ),
            SqlDelightDialectProvider().resolve(
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
                capabilities = setOf(DialectCapability.MySql),
                sourcePatterns = SqlDialectSourcePatterns.MySql,
            ),
            SqlDelightDialectProvider().resolve(
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
                capabilities = setOf(DialectCapability.PostgreSql),
                sourcePatterns = SqlDialectSourcePatterns.PostgreSql,
            ),
            SqlDelightDialectProvider().resolve(
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
                capabilities = setOf(DialectCapability.Hsql),
                sourcePatterns = SqlDialectSourcePatterns.Hsql,
            ),
            SqlDelightDialectProvider().resolve(
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
            SqlDelightDialectProvider().resolve(
                SqlDialectCoordinate(
                    group = "com.example",
                    module = "spanner-dialect",
                    version = "1.2.3",
                ),
            ),
        )
    }

    @Test
    fun `sqldelight dialect provider is visible to service loader`() {
        val providers = ServiceLoader.load(SqlDialectProvider::class.java).toList()

        assertNotNull(providers.firstOrNull { provider -> provider is SqlDelightDialectProvider })
    }
}
