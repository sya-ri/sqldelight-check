package dev.s7a.sqldelight.check.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SqlDialectSourcePatternExpressionTest {
    @Test
    fun `parses required optional and alternative pattern parts`() {
        val expression = SqlDialectSourcePatternExpression.parse("FETCH {FIRST|NEXT} [ROW]")

        assertEquals(
            listOf(
                SqlDialectSourcePatternPart(setOf("fetch")),
                SqlDialectSourcePatternPart(setOf("first", "next")),
                SqlDialectSourcePatternPart(setOf("row"), optional = true),
            ),
            expression.parts,
        )
    }

    @Test
    fun `normalizes parsed pattern part alternatives`() {
        val expression = SqlDialectSourcePatternExpression.parse("LEFT [OUTER] JOIN")

        assertEquals(setOf("left"), expression.parts[0].alternatives)
        assertFalse(expression.parts[0].optional)
        assertEquals(setOf("outer"), expression.parts[1].alternatives)
        assertTrue(expression.parts[1].optional)
        assertEquals(setOf("join"), expression.parts[2].alternatives)
        assertFalse(expression.parts[2].optional)
    }

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
    fun `matches optional term alternatives`() {
        val expression = SqlDialectSourcePatternExpression.parse("FETCH {FIRST|NEXT} [ROW|ROWS]")

        assertEquals(2, expression.matchPrefix(listOf("fetch", "first", "10")))
        assertEquals(3, expression.matchPrefix(listOf("fetch", "next", "row", "only")))
        assertEquals(3, expression.matchPrefix(listOf("fetch", "next", "rows", "only")))
        assertEquals(2, expression.matchPrefix(listOf("fetch", "next", "rowz", "only")))
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

    @Test
    fun `source pattern helpers support custom dialect definitions`() {
        val sourcePatterns =
            SqlDialectSourcePatterns(
                patterns =
                    SqlDialectSourcePatterns.SourceScannerDefault.patterns
                        .withoutExpressions("RIGHT [OUTER] JOIN") +
                        sourcePatterns(
                            "QUALIFY",
                            roles = setOf(SqlDialectSourcePatternRole.ClauseBoundary),
                        ),
            )

        assertNull(sourcePatterns.matchPrefix(SqlDialectSourcePatternRole.ClauseBoundary, listOf("right", "join", "team")))
        assertEquals(1, sourcePatterns.matchPrefix(SqlDialectSourcePatternRole.ClauseBoundary, listOf("qualify", "rank")))
    }

    @Test
    fun `source pattern helper attaches roles to every parsed expression`() {
        val patterns =
            sourcePatterns(
                "ORDER BY",
                "FETCH {FIRST|NEXT}",
                roles = setOf(SqlDialectSourcePatternRole.ClauseBoundary, SqlDialectSourcePatternRole.OrderByBoundary),
            )

        assertEquals(
            setOf(
                SqlDialectSourcePattern.parse(
                    "ORDER BY",
                    SqlDialectSourcePatternRole.ClauseBoundary,
                    SqlDialectSourcePatternRole.OrderByBoundary,
                ),
                SqlDialectSourcePattern.parse(
                    "FETCH {FIRST|NEXT}",
                    SqlDialectSourcePatternRole.ClauseBoundary,
                    SqlDialectSourcePatternRole.OrderByBoundary,
                ),
            ),
            patterns,
        )
    }

    @Test
    fun `without expressions removes matching parsed expressions only`() {
        val patterns =
            setOf(
                SqlDialectSourcePattern.parse("RIGHT", SqlDialectSourcePatternRole.TableReferenceBoundary),
                SqlDialectSourcePattern.parse("RIGHT [OUTER] JOIN", SqlDialectSourcePatternRole.ClauseBoundary),
                SqlDialectSourcePattern.parse("LEFT [OUTER] JOIN", SqlDialectSourcePatternRole.ClauseBoundary),
            )

        assertEquals(
            setOf(
                SqlDialectSourcePattern.parse("RIGHT", SqlDialectSourcePatternRole.TableReferenceBoundary),
                SqlDialectSourcePattern.parse("LEFT [OUTER] JOIN", SqlDialectSourcePatternRole.ClauseBoundary),
            ),
            patterns.withoutExpressions("RIGHT [OUTER] JOIN"),
        )
    }

    @Test
    fun `builtin sqlite patterns include broad sqlite family syntax`() {
        val sourcePatterns = SqlDialectSourcePatterns.SQLite

        assertTrue(sourcePatterns.matches(SqlDialectSourcePatternRole.SqlDelightExecutableStatementStart, listOf("replace")))
        assertTrue(sourcePatterns.matches(SqlDialectSourcePatternRole.StatementStart, listOf("pragma")))
        assertNull(sourcePatterns.matchPrefix(SqlDialectSourcePatternRole.ClauseBoundary, listOf("right", "join", "team")))
    }

    @Test
    fun `builtin mysql patterns include mysql family syntax`() {
        val sourcePatterns = SqlDialectSourcePatterns.MySql

        assertEquals(
            4,
            sourcePatterns.matchPrefix(
                SqlDialectSourcePatternRole.ClauseBoundary,
                listOf("on", "duplicate", "key", "update"),
            ),
        )
        assertTrue(sourcePatterns.matches(SqlDialectSourcePatternRole.DataTypeName, listOf("tinyint")))
        assertFalse(sourcePatterns.containsPattern("ON CONFLICT", SqlDialectSourcePatternRole.ClauseBoundary))
    }

    @Test
    fun `builtin postgresql patterns include postgresql family syntax`() {
        val sourcePatterns = SqlDialectSourcePatterns.PostgreSql

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
    }

    @Test
    fun `builtin hsql patterns include hsql family syntax`() {
        val sourcePatterns = SqlDialectSourcePatterns.Hsql

        assertTrue(sourcePatterns.matches(SqlDialectSourcePatternRole.SqlDelightExecutableStatementStart, listOf("merge")))
        assertEquals(
            3,
            sourcePatterns.matchPrefix(
                SqlDialectSourcePatternRole.ClauseBoundary,
                listOf("fetch", "next", "rows", "only"),
            ),
        )
        assertFalse(sourcePatterns.containsPattern("ON CONFLICT", SqlDialectSourcePatternRole.ClauseBoundary))
    }

    private fun SqlDialectSourcePatterns.containsPattern(
        expression: String,
        role: SqlDialectSourcePatternRole,
    ): Boolean =
        SqlDialectSourcePattern.parse(expression, role) in patternsFor(role)
}
