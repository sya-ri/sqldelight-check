package dev.s7a.sqldelight.check.dialects.hsql

import dev.s7a.sqldelight.check.api.DialectCapability
import dev.s7a.sqldelight.check.api.DialectFamily
import dev.s7a.sqldelight.check.api.SqlDialectCoordinate
import dev.s7a.sqldelight.check.api.SqlDialectProvider
import dev.s7a.sqldelight.check.api.SqlDialectSourcePattern
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole
import java.util.ServiceLoader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class HsqlDialectProviderTest {
    @Test
    fun `resolves SQLDelight HSQL dialect`() {
        assertEquals(
            HsqlDialect,
            HsqlDialectProvider().resolve(SqlDialectCoordinate("app.cash.sqldelight", "hsql-dialect", null)),
        )
        assertEquals(DialectFamily.Hsql, HsqlDialect.family)
        assertEquals(setOf(DialectCapability.Hsql), HsqlDialect.capabilities)
    }

    @Test
    fun `is discoverable by service loader`() {
        val providers = ServiceLoader.load(SqlDialectProvider::class.java).toList()

        assertNotNull(providers.firstOrNull { provider -> provider is HsqlDialectProvider })
    }

    @Test
    fun `source patterns include hsql family syntax`() {
        val sourcePatterns = HsqlDialectSourcePatterns

        assertTrue(sourcePatterns.matches(SqlDialectSourcePatternRole.SqlDelightExecutableStatementStart, listOf("merge")))
        assertEquals(
            2,
            sourcePatterns.matchPrefix(
                SqlDialectSourcePatternRole.ClauseBoundary,
                listOf("merge", "into", "player"),
            ),
        )
        assertEquals(
            3,
            sourcePatterns.matchPrefix(
                SqlDialectSourcePatternRole.ClauseBoundary,
                listOf("when", "not", "matched", "then"),
            ),
        )
        assertEquals(
            3,
            sourcePatterns.matchPrefix(
                SqlDialectSourcePatternRole.ClauseBoundary,
                listOf("fetch", "next", "rows", "only"),
            ),
        )
        assertTrue(sourcePatterns.matches(SqlDialectSourcePatternRole.DataTypeName, listOf("identity")))
        assertFalse(sourcePatterns.containsPattern("ON CONFLICT", SqlDialectSourcePatternRole.ClauseBoundary))
    }

    private fun dev.s7a.sqldelight.check.api.SqlDialectSourcePatterns.containsPattern(
        expression: String,
        role: SqlDialectSourcePatternRole,
    ): Boolean =
        SqlDialectSourcePattern.parse(expression, role) in patternsFor(role)
}
