package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.FixSafety
import kotlin.test.Test
import kotlin.test.assertEquals

class GroupByTargetNewlineRuleTest {
    @Test
    fun `reports multiline group by list with multiple targets on one line`() {
        val content =
            """
            selectScores:
            SELECT team_id, age, COUNT(*)
            FROM player
            GROUP BY team_id, age,
              active;
            """.asSqlDelightFile()
        val diagnostics = GroupByTargetNewlineRule().diagnostics(content)

        assertEquals(1, diagnostics.size)
        assertEquals(FixSafety.Safe, diagnostics.single().fixes.single().safety)
        GroupByTargetNewlineRule().assertAllFixes(
            content,
            """
            selectScores:
            SELECT team_id, age, COUNT(*)
            FROM player
            GROUP BY
            team_id,
            age,
              active;
            """.asSqlDelightFile(),
        )
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
