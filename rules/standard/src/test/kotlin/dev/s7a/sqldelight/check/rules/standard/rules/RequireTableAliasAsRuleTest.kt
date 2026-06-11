package dev.s7a.sqldelight.check.rules.standard.rules

import kotlin.test.Test
import kotlin.test.assertEquals

class RequireTableAliasAsRuleTest {
    @Test
    fun `reports implicit table aliases`() {
        val diagnostics =
            RequireTableAliasAsRule().diagnostics(
                """
                selectPlayers:
                SELECT p.id
                FROM player p
                JOIN team t ON t.id = p.team_id;
                """.asSqlDelightFile(),
            )

        assertEquals(2, diagnostics.size)
        assertEquals("Table aliases should use AS.", diagnostics.first().message)
    }

    @Test
    fun `reports implicit subquery aliases`() {
        RequireTableAliasAsRule().assertDiagnosticCount(
            """
            selectPlayers:
            SELECT ranked.id
            FROM (SELECT id FROM player) ranked;
            """.asSqlDelightFile(),
            1,
        )
    }

    @Test
    fun `accepts explicit aliases and unaliased tables`() {
        RequireTableAliasAsRule().assertDiagnosticCount(
            """
            selectPlayers:
            SELECT p.id, team.name
            FROM player AS p
            JOIN team ON team.id = p.team_id;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `ignores comments and strings`() {
        RequireTableAliasAsRule().assertDiagnosticCount(
            """
            selectPlayers:
            -- FROM player p
            SELECT 'FROM player p'
            FROM player AS p;
            """.asSqlDelightFile(),
            0,
        )
    }
}
