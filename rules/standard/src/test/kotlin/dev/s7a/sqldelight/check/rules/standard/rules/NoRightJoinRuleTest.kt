package dev.s7a.sqldelight.check.rules.standard.rules

import kotlin.test.Test
import kotlin.test.assertEquals

class NoRightJoinRuleTest {
    @Test
    fun `reports right join`() {
        val diagnostics =
            NoRightJoinRule().diagnostics(
                """
                selectPlayerTeams:
                SELECT player.id, team.name
                FROM player
                RIGHT JOIN team ON team.id = player.team_id;
                """.asSqlDelightFile(),
            )

        assertEquals(1, diagnostics.size)
        assertEquals(0, diagnostics.single().fixes.size)
    }

    @Test
    fun `reports right outer join in migration files`() {
        val diagnostics =
            NoRightJoinRule().diagnostics(
                """
                INSERT INTO player_team_snapshot(player_id, team_name)
                SELECT player.id, team.name
                FROM player
                RIGHT OUTER JOIN team ON team.id = player.team_id;
                """.asSqlDelightFile(),
                path = MIGRATION_SQM_PATH,
            )

        assertEquals(1, diagnostics.size)
    }

    @Test
    fun `accepts left join and inner join`() {
        NoRightJoinRule().assertDiagnosticCount(
            """
            selectPlayerTeams:
            SELECT player.id, team.name
            FROM player
            LEFT JOIN team ON team.id = player.team_id
            INNER JOIN league ON league.id = team.league_id;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `ignores comments strings and quoted identifiers`() {
        NoRightJoinRule().assertDiagnosticCount(
            """
            selectLiteral:
            -- RIGHT JOIN
            SELECT 'RIGHT JOIN', "RIGHT JOIN", `RIGHT JOIN`, [RIGHT JOIN]
            FROM player;
            """.asSqlDelightFile(),
            0,
        )
    }
}
