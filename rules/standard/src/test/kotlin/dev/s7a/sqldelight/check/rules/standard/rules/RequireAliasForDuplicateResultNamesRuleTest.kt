package dev.s7a.sqldelight.check.rules.standard.rules

import kotlin.test.Test
import kotlin.test.assertEquals

class RequireAliasForDuplicateResultNamesRuleTest {
    @Test
    fun `reports duplicate visible result names`() {
        val diagnostics =
            RequireAliasForDuplicateResultNamesRule().diagnostics(
                """
                playerWithTeam:
                SELECT player.name, team.name
                FROM player
                JOIN team;
                """.asSqlDelightFile(),
            )

        assertEquals(1, diagnostics.size)
        assertEquals(0, diagnostics.single().fixes.size)
    }

    @Test
    fun `accepts disambiguated result aliases`() {
        RequireAliasForDuplicateResultNamesRule().assertDiagnosticCount(
            """
            playerWithTeam:
            SELECT player.name AS player_name, team.name AS team_name
            FROM player
            JOIN team;
            """.asSqlDelightFile(),
            expected = 0,
        )
    }
}
