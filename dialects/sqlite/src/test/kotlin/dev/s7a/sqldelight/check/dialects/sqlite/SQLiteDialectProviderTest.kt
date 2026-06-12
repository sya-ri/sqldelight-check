package dev.s7a.sqldelight.check.dialects.sqlite

import dev.s7a.sqldelight.check.api.DialectCapability
import dev.s7a.sqldelight.check.api.DialectFamily
import dev.s7a.sqldelight.check.api.SqlDialectCoordinate
import dev.s7a.sqldelight.check.api.SqlDialectProvider
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole
import java.util.ServiceLoader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SQLiteDialectProviderTest {
    @Test
    fun `resolves SQLDelight SQLite dialects`() {
        assertEquals(
            SQLiteDialect,
            SQLiteDialectProvider().resolve(SqlDialectCoordinate("app.cash.sqldelight", "sqlite-3-38-dialect", null)),
        )
        assertEquals(DialectFamily.SQLite, SQLiteDialect.family)
        assertEquals(setOf(DialectCapability.SQLite), SQLiteDialect.capabilities)
    }

    @Test
    fun `does not resolve non SQLite SQLDelight dialects`() {
        assertNull(SQLiteDialectProvider().resolve(SqlDialectCoordinate("app.cash.sqldelight", "mysql-dialect", null)))
    }

    @Test
    fun `is discoverable by service loader`() {
        val providers = ServiceLoader.load(SqlDialectProvider::class.java).toList()

        assertNotNull(providers.firstOrNull { provider -> provider is SQLiteDialectProvider })
    }

    @Test
    fun `source patterns include sqlite family syntax`() {
        val sourcePatterns = SQLiteDialectSourcePatterns

        assertTrue(sourcePatterns.matches(SqlDialectSourcePatternRole.SqlDelightExecutableStatementStart, listOf("replace")))
        assertTrue(sourcePatterns.matches(SqlDialectSourcePatternRole.StatementStart, listOf("pragma")))
        assertTrue(sourcePatterns.matches(SqlDialectSourcePatternRole.DataTypeName, listOf("json")))
        assertTrue(sourcePatterns.matches(SqlDialectSourcePatternRole.KeywordCaseTarget, listOf("excluded")))
        assertNull(sourcePatterns.matchPrefix(SqlDialectSourcePatternRole.ClauseBoundary, listOf("right", "join", "team")))
    }
}
