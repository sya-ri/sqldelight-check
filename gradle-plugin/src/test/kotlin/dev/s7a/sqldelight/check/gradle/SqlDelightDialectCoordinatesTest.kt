package dev.s7a.sqldelight.check.gradle

import dev.s7a.sqldelight.check.api.DialectCapabilities
import dev.s7a.sqldelight.check.api.DialectFamily
import dev.s7a.sqldelight.check.api.SqlDialect
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for SQLDelight dialect coordinate mapping.
 */
class SqlDelightDialectCoordinatesTest {
    @Test
    fun `maps official sqldelight dialect artifacts`() {
        assertEquals(
            SqlDialect(
                family = DialectFamily.SQLite,
                displayName = "sqlite 3 38",
                artifact = "app.cash.sqldelight:sqlite-3-38-dialect",
                version = "2.3.2",
                capabilities = setOf(DialectCapabilities.SQLite),
            ),
            sqlDialectFromCoordinate(
                group = "app.cash.sqldelight",
                module = "sqlite-3-38-dialect",
                version = "2.3.2",
            ),
        )
        assertEquals(
            SqlDialect(
                family = DialectFamily.MySql,
                displayName = "MySQL",
                artifact = "app.cash.sqldelight:mysql-dialect",
                version = "2.3.2",
                capabilities = setOf(DialectCapabilities.MySql),
            ),
            sqlDialectFromCoordinate(
                group = "app.cash.sqldelight",
                module = "mysql-dialect",
                version = "2.3.2",
            ),
        )
        assertEquals(
            SqlDialect(
                family = DialectFamily.PostgreSql,
                displayName = "PostgreSQL",
                artifact = "app.cash.sqldelight:postgresql-dialect",
                version = "2.3.2",
                capabilities = setOf(DialectCapabilities.PostgreSql),
            ),
            sqlDialectFromCoordinate(
                group = "app.cash.sqldelight",
                module = "postgresql-dialect",
                version = "2.3.2",
            ),
        )
        assertEquals(
            SqlDialect(
                family = DialectFamily.Hsql,
                displayName = "HSQL",
                artifact = "app.cash.sqldelight:hsql-dialect",
                version = "2.3.2",
                capabilities = setOf(DialectCapabilities.Hsql),
            ),
            sqlDialectFromCoordinate(
                group = "app.cash.sqldelight",
                module = "hsql-dialect",
                version = "2.3.2",
            ),
        )
    }

    @Test
    fun `falls back to custom dialect metadata for third party artifacts`() {
        assertEquals(
            SqlDialect(
                family = DialectFamily.Custom,
                displayName = "spanner-dialect",
                artifact = "com.example:spanner-dialect",
                version = "1.2.3",
            ),
            sqlDialectFromCoordinate(
                group = "com.example",
                module = "spanner-dialect",
                version = "1.2.3",
            ),
        )
    }
}
