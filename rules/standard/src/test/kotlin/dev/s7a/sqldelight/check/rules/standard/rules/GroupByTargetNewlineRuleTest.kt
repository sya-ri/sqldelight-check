package dev.s7a.sqldelight.check.rules.standard.rules

import kotlin.test.Test
import kotlin.test.assertEquals

class GroupByTargetNewlineRuleTest {
    @Test
    fun `reports multiline group by list with multiple targets on one line`() {
        val diagnostics =
            GroupByTargetNewlineRule().diagnostics(
                """
                selectScores:
                SELECT team_id, age, COUNT(*)
                FROM player
                GROUP BY team_id, age,
                  active;
                """.asSqlDelightFile(),
            )

        assertEquals(1, diagnostics.size)
        assertEquals(0, diagnostics.single().fixes.size)
    }

    @Test
    fun `accepts one group by target per line`() {
        GroupByTargetNewlineRule().assertDiagnosticCount(
            """
            selectScores:
            SELECT team_id, age, COUNT(*)
            FROM player
            GROUP BY
              team_id,
              age,
              active;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `accepts single line group by lists`() {
        GroupByTargetNewlineRule().assertDiagnosticCount(
            """
            selectScores:
            SELECT team_id, age, COUNT(*) FROM player GROUP BY team_id, age;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `ignores commas in nested expressions`() {
        GroupByTargetNewlineRule().assertDiagnosticCount(
            """
            selectScores:
            SELECT COALESCE(team_id, -1), age, COUNT(*)
            FROM player
            GROUP BY
              COALESCE(team_id, -1),
              age;
            """.asSqlDelightFile(),
            0,
        )
    }
}
