package dev.s7a.sqldelight.check.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SqlDialectSourcePatternExpressionTest {
    @Test
    fun `matches required terms`() {
        val expression = SqlDialectSourcePatternExpression.parse("ORDER BY")

        assertEquals(2, expression.matchPrefix(listOf("order", "by", "name")))
        assertNull(expression.matchPrefix(listOf("order")))
    }

    @Test
    fun `matches optional terms`() {
        val expression = SqlDialectSourcePatternExpression.parse("LEFT [OUTER] JOIN")

        assertEquals(2, expression.matchPrefix(listOf("left", "join")))
        assertEquals(3, expression.matchPrefix(listOf("left", "outer", "join")))
    }

    @Test
    fun `matches term alternatives`() {
        val expression = SqlDialectSourcePatternExpression.parse("FETCH {FIRST|NEXT} [ROW]")

        assertEquals(2, expression.matchPrefix(listOf("fetch", "first", "10")))
        assertEquals(3, expression.matchPrefix(listOf("fetch", "next", "row", "only")))
        assertNull(expression.matchPrefix(listOf("fetch", "prior")))
    }

    @Test
    fun `source patterns choose longest prefix for role`() {
        val sourcePatterns =
            SqlDialectSourcePatterns(
                patterns =
                    setOf(
                        SqlDialectSourcePattern.parse("ORDER", SqlDialectSourcePatternRole.ClauseBoundary),
                        SqlDialectSourcePattern.parse("ORDER BY", SqlDialectSourcePatternRole.ClauseBoundary),
                    ),
            )

        assertEquals(2, sourcePatterns.matchPrefix(SqlDialectSourcePatternRole.ClauseBoundary, listOf("order", "by", "name")))
    }
}
