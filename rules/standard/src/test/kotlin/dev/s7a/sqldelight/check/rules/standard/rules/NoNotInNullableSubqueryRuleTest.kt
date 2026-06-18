package dev.s7a.sqldelight.check.rules.standard.rules

import kotlin.test.Test
import kotlin.test.assertEquals

class NoNotInNullableSubqueryRuleTest {
    @Test
    fun `reports not in subqueries without null exclusion`() {
        val content =
            """
            selectTeams:
            SELECT id
            FROM team
            WHERE id NOT IN (
              SELECT team_id
              FROM player
            );
            """.asSqlDelightFile()
        val diagnostics =
            NoNotInNullableSubqueryRule().diagnostics(
                content,
            )

        assertEquals(1, diagnostics.size)
        assertEquals(0, diagnostics.single().fixes.size)
    }

    @Test
    fun `accepts not exists and not in subqueries with null exclusion`() {
        NoNotInNullableSubqueryRule().assertDiagnosticCount(
            """
            selectTeams:
            SELECT id
            FROM team
            WHERE NOT EXISTS (
              SELECT 1
              FROM player
              WHERE player.team_id = team.id
            )
            AND id NOT IN (
              SELECT team_id
              FROM player
              WHERE team_id IS NOT NULL
            );
            """.asSqlDelightFile(),
            expected = 0,
        )
    }
}
