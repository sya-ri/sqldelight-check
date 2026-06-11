package dev.s7a.sqldelight.check.rules.standard.rules

import kotlin.test.Test
import kotlin.test.assertEquals

class RequireResultColumnAliasRuleTest {
    @Test
    fun `reports computed result column without alias`() {
        val diagnostics =
            RequireResultColumnAliasRule().diagnostics(
                """
                selectStats:
                SELECT id, count(*), score + 1 AS next_score, max(score) max_score
                FROM player;
                """.asSqlDelightFile(),
            )

        assertEquals(1, diagnostics.size)
        assertEquals(2, diagnostics.single().range?.start?.line)
    }

    @Test
    fun `accepts simple columns and aliased computed columns`() {
        RequireResultColumnAliasRule().assertDiagnosticCount(
            """
            selectStats:
            SELECT id, player.name, count(*) AS score_count, score + 1 next_score
            FROM player;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `reports case expression without alias`() {
        RequireResultColumnAliasRule().assertDiagnosticCount(
            """
            selectBucket:
            SELECT CASE WHEN score > 0 THEN 1 ELSE 0 END
            FROM player;
            """.asSqlDelightFile(),
            1,
        )
    }
}
