package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.DialectId
import dev.s7a.sqldelight.check.api.SqlDialect
import dev.s7a.sqldelight.check.api.SqlDialectSourceBlockPatterns
import dev.s7a.sqldelight.check.api.SqlDialectSourceParenthesisDepthTerms
import dev.s7a.sqldelight.check.api.SqlDialectSourceParenthesizedBlockPattern
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatterns
import dev.s7a.sqldelight.check.api.SqlSourceBlockKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MaxSubqueryDepthRuleTest {
    @Test
    fun `reports select deeper than configured max depth`() {
        val diagnostics =
            MaxSubqueryDepthRule().diagnostics(
                """
                selectNested:
                SELECT id
                FROM player
                WHERE id IN (
                  SELECT player_id
                  FROM score
                  WHERE score IN (
                    SELECT value
                    FROM score_limit
                  )
                );
                """.asSqlDelightFile(),
                options = mapOf("maxDepth" to "1"),
            )

        assertEquals(1, diagnostics.size)
        assertEquals(8, diagnostics.single().range?.start?.line)
    }

    @Test
    fun `accepts select at configured max depth`() {
        MaxSubqueryDepthRule().assertDiagnosticCount(
            """
            selectNested:
            SELECT id
            FROM player
            WHERE id IN (
              SELECT player_id
              FROM score
            );
            """.asSqlDelightFile(),
            0,
            options = mapOf("maxDepth" to "1"),
        )
    }

    @Test
    fun `does not count parenthesized expressions as subquery depth`() {
        MaxSubqueryDepthRule().assertDiagnosticCount(
            """
            selectNested:
            SELECT id
            FROM player
            WHERE EXISTS (
                ((
                    SELECT score
                    FROM score
                ))
            );
            """.asSqlDelightFile(),
            0,
            options = mapOf("maxDepth" to "1"),
        )
    }

    @Test
    fun `uses dialect source block patterns for subquery depth`() {
        val diagnostics =
            MaxSubqueryDepthRule().diagnostics(
                """
                selectNested:
                SELECT id
                FROM player
                WHERE EXISTS {
                    SELECT player_id
                    FROM score
                    WHERE score IN {
                        SELECT value
                        FROM score_limit
                    }
                };
                """.asSqlDelightFile(),
                options = mapOf("maxDepth" to "1"),
                dialect = braceDialect,
            )

        assertEquals(1, diagnostics.size)
        assertEquals(8, diagnostics.single().range?.start?.line)
    }

    @Test
    fun `rejects invalid max depth option`() {
        assertFailsWith<IllegalArgumentException> {
            MaxSubqueryDepthRule().diagnostics(cleanPlayerSq, options = mapOf("maxDepth" to "0"))
        }
    }
}

private val braceDialect =
    SqlDialect(
        ids = setOf(DialectId("default")),
        sourcePatterns =
            SqlDialectSourcePatterns(
                blockPatterns =
                    SqlDialectSourceBlockPatterns(
                        parenthesisDepthTerms =
                            setOf(
                                SqlDialectSourceParenthesisDepthTerms(
                                    openTerm = "{",
                                    closeTerm = "}",
                                ),
                            ),
                        parenthesizedBlocks =
                            setOf(
                                SqlDialectSourceParenthesizedBlockPattern(
                                    openTerm = "{",
                                    closeTerm = "}",
                                    defaultKind = SqlSourceBlockKind.ParenthesizedExpression,
                                    innerStartRoles = setOf(SqlDialectSourcePatternRole.SelectListStart),
                                    innerStartKind = SqlSourceBlockKind.Subquery,
                                ),
                            ),
                    ),
            ),
    )
