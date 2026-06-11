package dev.s7a.sqldelight.check.rules.standard.rules

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
    fun `rejects invalid max depth option`() {
        assertFailsWith<IllegalArgumentException> {
            MaxSubqueryDepthRule().diagnostics(cleanPlayerSq, options = mapOf("maxDepth" to "0"))
        }
    }
}
