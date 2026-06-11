package dev.s7a.sqldelight.check.rules.standard.rules

import kotlin.test.Test
import kotlin.test.assertEquals

class ExplicitCrossJoinRuleTest {
    @Test
    fun `reports bare join without on or using`() {
        val diagnostics =
            ExplicitCrossJoinRule().diagnostics(
                """
                selectPlayerTeams:
                SELECT player.id, team.name
                FROM player
                JOIN team;
                """.asSqlDelightFile(),
            )

        assertEquals(1, diagnostics.size)
        assertEquals(0, diagnostics.single().fixes.size)
    }

    @Test
    fun `reports typed join without condition in migration files`() {
        val diagnostics =
            ExplicitCrossJoinRule().diagnostics(
                """
                INSERT INTO player_team_snapshot(player_id, team_name)
                SELECT player.id, team.name
                FROM player
                INNER JOIN team;
                """.asSqlDelightFile(),
                path = MIGRATION_SQM_PATH,
            )

        assertEquals(1, diagnostics.size)
    }

    @Test
    fun `accepts cross natural and conditioned joins`() {
        ExplicitCrossJoinRule().assertDiagnosticCount(
            """
            selectPlayerTeams:
            SELECT player.id, team.name
            FROM player
            CROSS JOIN team
            NATURAL JOIN league
            LEFT JOIN coach ON coach.team_id = team.id
            RIGHT JOIN sponsor USING (team_id)
            FULL JOIN arena ON arena.team_id = team.id
            INNER JOIN division ON division.id = team.division_id;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `does not use subquery on as join condition`() {
        ExplicitCrossJoinRule().assertDiagnosticCount(
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
            1,
        )
    }

    @Test
    fun `ignores comments strings and quoted identifiers`() {
        ExplicitCrossJoinRule().assertDiagnosticCount(
            """
            selectLiteral:
            -- JOIN team
            SELECT 'JOIN', "JOIN", `JOIN`, [JOIN]
            FROM player;
            """.asSqlDelightFile(),
            0,
        )
    }
}
