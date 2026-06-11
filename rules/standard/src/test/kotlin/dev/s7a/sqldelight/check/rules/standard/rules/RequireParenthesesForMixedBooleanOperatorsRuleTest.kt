package dev.s7a.sqldelight.check.rules.standard.rules

import kotlin.test.Test
import kotlin.test.assertEquals

class RequireParenthesesForMixedBooleanOperatorsRuleTest {
    @Test
    fun `reports mixed and and or in where predicates`() {
        val diagnostics =
            RequireParenthesesForMixedBooleanOperatorsRule().diagnostics(
                """
                selectPlayers:
                SELECT id
                FROM player
                WHERE active = 1
                  AND deleted_at IS NULL
                  OR admin = 1;
                """.asSqlDelightFile(),
            )

        assertEquals(1, diagnostics.size)
        assertEquals(0, diagnostics.single().fixes.size)
    }

    @Test
    fun `accepts grouped mixed boolean predicates`() {
        RequireParenthesesForMixedBooleanOperatorsRule().assertDiagnosticCount(
            """
            selectPlayers:
            SELECT id
            FROM player
            WHERE (active = 1 AND deleted_at IS NULL)
              OR admin = 1;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `accepts alternative grouped mixed boolean predicates`() {
        RequireParenthesesForMixedBooleanOperatorsRule().assertDiagnosticCount(
            """
            selectPlayers:
            SELECT id
            FROM player
            WHERE active = 1
              AND (deleted_at IS NULL OR admin = 1);
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `reports mixed boolean operators inside a parenthesized group`() {
        RequireParenthesesForMixedBooleanOperatorsRule().assertDiagnosticCount(
            """
            selectPlayers:
            SELECT id
            FROM player
            WHERE (active = 1 AND deleted_at IS NULL OR admin = 1);
            """.asSqlDelightFile(),
            1,
        )
    }

    @Test
    fun `ignores and inside between expressions`() {
        RequireParenthesesForMixedBooleanOperatorsRule().assertDiagnosticCount(
            """
            selectPlayers:
            SELECT id
            FROM player
            WHERE score BETWEEN 1 AND 10
              OR admin = 1;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `reports mixed boolean operators after between expressions`() {
        RequireParenthesesForMixedBooleanOperatorsRule().assertDiagnosticCount(
            """
            selectPlayers:
            SELECT id
            FROM player
            WHERE score BETWEEN 1 AND 10
              AND active = 1
              OR admin = 1;
            """.asSqlDelightFile(),
            1,
        )
    }

    @Test
    fun `reports mixed boolean operators in having predicates`() {
        RequireParenthesesForMixedBooleanOperatorsRule().assertDiagnosticCount(
            """
            selectPlayers:
            SELECT team_id, COUNT(*)
            FROM player
            GROUP BY team_id
            HAVING COUNT(*) > 1
              AND team_id IS NOT NULL
              OR team_id = 0;
            """.asSqlDelightFile(),
            1,
        )
    }

    @Test
    fun `reports mixed boolean operators in join predicates`() {
        RequireParenthesesForMixedBooleanOperatorsRule().assertDiagnosticCount(
            """
            selectPlayers:
            SELECT player.id
            FROM player
            INNER JOIN team
              ON team.id = player.team_id
              AND team.active = 1
              OR player.admin = 1;
            """.asSqlDelightFile(),
            1,
        )
    }

    @Test
    fun `accepts predicates with only one boolean operator kind`() {
        RequireParenthesesForMixedBooleanOperatorsRule().assertDiagnosticCount(
            """
            selectPlayers:
            SELECT id
            FROM player
            WHERE active = 1
              AND deleted_at IS NULL
              AND team_id IS NOT NULL;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `ignores comments strings and quoted identifiers`() {
        RequireParenthesesForMixedBooleanOperatorsRule().assertDiagnosticCount(
            """
            selectLiteral:
            -- WHERE active = 1 AND deleted_at IS NULL OR admin = 1
            SELECT 'WHERE active = 1 AND deleted_at IS NULL OR admin = 1',
              "WHERE active = 1 AND deleted_at IS NULL OR admin = 1",
              `WHERE active = 1 AND deleted_at IS NULL OR admin = 1`,
              [WHERE active = 1 AND deleted_at IS NULL OR admin = 1]
            FROM player;
            """.asSqlDelightFile(),
            0,
        )
    }
}
