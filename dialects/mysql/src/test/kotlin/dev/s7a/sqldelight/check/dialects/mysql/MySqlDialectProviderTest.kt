package dev.s7a.sqldelight.check.dialects.mysql

import dev.s7a.sqldelight.check.api.DialectId
import dev.s7a.sqldelight.check.dialects.mysql.MySqlDialectId
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

class MySqlDialectProviderTest {
    @Test
    fun `resolves SQLDelight MySQL dialect`() {
        assertEquals(
            MySqlDialect,
            MySqlDialectProvider().resolve(SqlDialectCoordinate("app.cash.sqldelight", "mysql-dialect", null)),
        )
        assertEquals(setOf(MySqlDialectId), MySqlDialect.ids)
    }

    @Test
    fun `is discoverable by service loader`() {
        val providers = ServiceLoader.load(SqlDialectProvider::class.java).toList()

        assertNotNull(providers.firstOrNull { provider -> provider is MySqlDialectProvider })
    }

    @Test
    fun `source patterns include mysql dialect syntax`() {
        val sourcePatterns = MySqlDialectSourcePatterns

        assertTrue(sourcePatterns.matches(SqlDialectSourcePatternRole.StatementStart, listOf("show")))
        assertEquals(
            4,
            sourcePatterns.matchPrefix(
                SqlDialectSourcePatternRole.ClauseBoundary,
                listOf("on", "duplicate", "key", "update"),
            ),
        )
        assertEquals(
            4,
            sourcePatterns.matchPrefix(
                SqlDialectSourcePatternRole.ClauseBoundary,
                listOf("lock", "in", "share", "mode"),
            ),
        )
        assertTrue(sourcePatterns.matches(SqlDialectSourcePatternRole.DataTypeName, listOf("tinyint")))
        assertTrue(sourcePatterns.matches(SqlDialectSourcePatternRole.CommonFunctionName, listOf("date_format")))
        assertTrue(sourcePatterns.matches(SqlDialectSourcePatternRole.KeywordCaseTarget, listOf("unsigned")))
        assertFalse(sourcePatterns.containsPattern("ON CONFLICT", SqlDialectSourcePatternRole.ClauseBoundary))
    }

    private fun dev.s7a.sqldelight.check.api.SqlDialectSourcePatterns.containsPattern(
        expression: String,
        role: SqlDialectSourcePatternRole,
    ): Boolean =
        SqlDialectSourcePattern.parse(expression, role) in patternsFor(role)
}
