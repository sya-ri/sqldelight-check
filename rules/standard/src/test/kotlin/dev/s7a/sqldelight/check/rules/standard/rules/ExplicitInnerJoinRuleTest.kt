package dev.s7a.sqldelight.check.rules.standard.rules

import kotlin.test.Test
import kotlin.test.assertEquals

class ExplicitInnerJoinRuleTest {
    @Test
    fun `reports bare join with on or using`() {
        val diagnostics =
            ExplicitInnerJoinRule().diagnostics(
                """
                selectPlayerTeams:
                SELECT player.id, team.name
                FROM player
                JOIN team ON team.id = player.team_id
                JOIN league USING (league_id);
                """.asSqlDelightFile(),
            )

        assertEquals(2, diagnostics.size)
        assertEquals(0, diagnostics.first().fixes.size)
    }

    @Test
    fun `reports bare join in migration files`() {
        ExplicitInnerJoinRule().assertDiagnosticCount(
            """
            INSERT INTO player_team_snapshot(player_id, team_name)
            SELECT player.id, team.name
            FROM player
            JOIN team ON team.id = player.team_id;
            """.asSqlDelightFile(),
            1,
            path = MIGRATION_SQM_PATH,
        )
    }

    @Test
    fun `accepts explicit join types and conditionless joins`() {
        ExplicitInnerJoinRule().assertDiagnosticCount(
            """
            selectPlayerTeams:
            SELECT player.id, team.name
            FROM player
            INNER JOIN team ON team.id = player.team_id
            LEFT JOIN coach ON coach.team_id = team.id
            LEFT OUTER JOIN sponsor USING (team_id)
            RIGHT JOIN arena ON arena.team_id = team.id
            FULL OUTER JOIN division ON division.id = team.division_id
            CROSS JOIN region
            NATURAL JOIN league
            JOIN country;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `does not use subquery on as join condition`() {
        ExplicitInnerJoinRule().assertDiagnosticCount(
            """
            selectPlayerTeams:
            SELECT player.id, team.name
            FROM player
            JOIN (
              SELECT id
              FROM team
              WHERE EXISTS (
                SELECT 1
                FROM sponsor
                WHERE sponsor.team_id = team.id
              )
            ) AS team;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `ignores comments strings and quoted identifiers`() {
        ExplicitInnerJoinRule().assertDiagnosticCount(
            """
            selectLiteral:
            -- JOIN team ON team.id = player.team_id
            SELECT 'JOIN team ON team.id', "JOIN", `JOIN`, [JOIN]
            FROM player;
            """.asSqlDelightFile(),
            0,
        )
    }
}
