package dev.s7a.sqldelight.check.rules.standard.rules

import kotlin.test.Test
import kotlin.test.assertEquals

class NoImplicitCrossJoinCommaRuleTest {
    @Test
    fun `reports comma-separated from sources`() {
        val diagnostics =
            NoImplicitCrossJoinCommaRule().diagnostics(
                """
                selectPlayerTeams:
                SELECT player.id, team.name
                FROM player, team;
                """.asSqlDelightFile(),
            )

        assertEquals(1, diagnostics.size)
        assertEquals(0, diagnostics.single().fixes.size)
    }

    @Test
    fun `reports each top-level comma in from list`() {
        NoImplicitCrossJoinCommaRule().assertDiagnosticCount(
            """
            selectPlayerTeams:
            SELECT player.id, team.name, league.name
            FROM player, team, league;
            """.asSqlDelightFile(),
            expected = 2,
        )
    }

    @Test
    fun `accepts explicit joins`() {
        NoImplicitCrossJoinCommaRule().assertDiagnosticCount(
            """
            selectPlayerTeams:
            SELECT player.id, team.name
            FROM player
            CROSS JOIN team
            INNER JOIN league ON league.id = team.league_id;
            """.asSqlDelightFile(),
            expected = 0,
        )
    }

    @Test
    fun `ignores commas outside from sources`() {
        NoImplicitCrossJoinCommaRule().assertDiagnosticCount(
            """
            selectPlayerTeams:
            SELECT player.id, team.name, COALESCE(team.name, league.name)
            FROM player
            INNER JOIN team ON team.id = player.team_id
            WHERE player.id IN (1, 2, 3)
            ORDER BY team.name, player.id;
            """.asSqlDelightFile(),
            expected = 0,
        )
    }

    @Test
    fun `reports comma joins inside nested subqueries`() {
        NoImplicitCrossJoinCommaRule().assertDiagnosticCount(
            """
            selectPlayers:
            SELECT player.id
            FROM player
            WHERE EXISTS (
              SELECT 1
              FROM team, league
              WHERE league.id = team.league_id
            );
            """.asSqlDelightFile(),
            expected = 1,
        )
    }

    @Test
    fun `ignores comments strings and quoted identifiers`() {
        NoImplicitCrossJoinCommaRule().assertDiagnosticCount(
            """
            selectLiteral:
            -- FROM player, team
            SELECT 'FROM player, team', "FROM", `FROM`, [FROM]
            FROM player;
            """.asSqlDelightFile(),
            expected = 0,
        )
    }
}
