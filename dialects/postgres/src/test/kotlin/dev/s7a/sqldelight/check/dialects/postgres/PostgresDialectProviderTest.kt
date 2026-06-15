package dev.s7a.sqldelight.check.dialects.postgres

import dev.s7a.sqldelight.check.api.DialectId
import dev.s7a.sqldelight.check.dialects.postgres.PostgresDialectId
import dev.s7a.sqldelight.check.api.SqlDialectCoordinate
import dev.s7a.sqldelight.check.api.SqlDialectProvider
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole
import java.util.ServiceLoader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PostgresDialectProviderTest {
    @Test
    fun `resolves SQLDelight PostgreSQL dialect`() {
        assertEquals(
            PostgresDialect,
            PostgresDialectProvider().resolve(SqlDialectCoordinate("app.cash.sqldelight", "postgresql-dialect", null)),
        )
        assertEquals(setOf(PostgresDialectId), PostgresDialect.ids)
    }

    @Test
    fun `is discoverable by service loader`() {
        val providers = ServiceLoader.load(SqlDialectProvider::class.java).toList()

        assertNotNull(providers.firstOrNull { provider -> provider is PostgresDialectProvider })
    }

    @Test
    fun `source patterns include postgresql dialect syntax`() {
        val sourcePatterns = PostgresDialectSourcePatterns

        assertTrue(sourcePatterns.matches(SqlDialectSourcePatternRole.StatementStart, listOf("copy")))
        assertEquals(
            2,
            sourcePatterns.matchPrefix(
                SqlDialectSourcePatternRole.ClauseBoundary,
                listOf("distinct", "on", "(", "id", ")"),
            ),
        )
        assertEquals(
            4,
            sourcePatterns.matchPrefix(
                SqlDialectSourcePatternRole.ClauseBoundary,
                listOf("for", "no", "key", "update"),
            ),
        )
        assertEquals(
            3,
            sourcePatterns.matchPrefix(
                SqlDialectSourcePatternRole.ClauseBoundary,
                listOf("fetch", "first", "rows", "only"),
            ),
        )
        assertTrue(sourcePatterns.matches(SqlDialectSourcePatternRole.DataTypeName, listOf("uuid")))
        assertTrue(sourcePatterns.matches(SqlDialectSourcePatternRole.CommonFunctionName, listOf("json_agg")))
        assertTrue(sourcePatterns.matches(SqlDialectSourcePatternRole.AliasBoundary, listOf("lateral")))
        assertTrue(sourcePatterns.matches(SqlDialectSourcePatternRole.OrderByBoundary, listOf("returning")))
        assertTrue(sourcePatterns.matches(SqlDialectSourcePatternRole.StatementContinuation, listOf("update", "set")))
        assertTrue(sourcePatterns.matches(SqlDialectSourcePatternRole.StatementContinuation, listOf("insert", "set")))
    }
}
