package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.FixSafety
import kotlin.test.Test
import kotlin.test.assertEquals

class ExplicitCrossJoinRuleTest {
    @Test
    fun `reports bare join without on or using`() {
        val content =
            """
            selectPlayerTeams:
            SELECT player.id, team.name
            FROM player
            JOIN team;
            """.asSqlDelightFile()
        val diagnostics = ExplicitCrossJoinRule().diagnostics(content)

        assertEquals(1, diagnostics.size)
        assertEquals(FixSafety.Unsafe, diagnostics.single().fixes.single().safety)
        ExplicitCrossJoinRule().assertAllFixes(
            content,
            """
            selectPlayerTeams:
            SELECT player.id, team.name
            FROM player
            CROSS JOIN team;
            """.asSqlDelightFile(),
        )
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
    fun `fixes typed joins without conditions as cross joins`() {
        val content =
            """
            INSERT INTO player_team_snapshot(player_id, team_name)
            SELECT player.id, team.name
            FROM player
            INNER JOIN team;
            """.asSqlDelightFile()

        ExplicitCrossJoinRule().assertAllFixes(
            content,
            """
            INSERT INTO player_team_snapshot(player_id, team_name)
            SELECT player.id, team.name
            FROM player
            CROSS JOIN team;
            """.asSqlDelightFile(),
            path = MIGRATION_SQM_PATH,
        )
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
