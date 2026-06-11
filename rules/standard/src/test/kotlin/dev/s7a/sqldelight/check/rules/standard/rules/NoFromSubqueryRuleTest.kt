package dev.s7a.sqldelight.check.rules.standard.rules

import kotlin.test.Test
import kotlin.test.assertEquals

class NoFromSubqueryRuleTest {
    @Test
    fun `reports top level from subqueries`() {
        val diagnostics =
            NoFromSubqueryRule().diagnostics(
                """
                selectPlayers:
                SELECT ranked.id
                FROM (SELECT id FROM player) AS ranked;
                """.asSqlDelightFile(),
            )

        assertEquals(1, diagnostics.size)
        assertEquals(0, diagnostics.single().fixes.size)
    }

    @Test
    fun `reports top level join subqueries`() {
        NoFromSubqueryRule().assertDiagnosticCount(
            """
            selectPlayers:
            SELECT player.id
            FROM player
            JOIN (SELECT player_id FROM score) AS recent_score
              ON recent_score.player_id = player.id;
            """.asSqlDelightFile(),
            1,
        )
    }

    @Test
    fun `reports from subqueries in migration files`() {
        NoFromSubqueryRule().assertDiagnosticCount(
            """
            INSERT INTO player_backup(id)
            SELECT id
            FROM (SELECT id FROM player) AS active_player;
            """.asSqlDelightFile(),
            1,
            path = MIGRATION_SQM_PATH,
        )
    }

    @Test
    fun `accepts common table expressions and ordinary tables`() {
        NoFromSubqueryRule().assertDiagnosticCount(
            """
            selectPlayers:
            WITH ranked AS (
              SELECT id
              FROM player
            )
            SELECT ranked.id
            FROM ranked
            JOIN score ON score.player_id = ranked.id;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `ignores nested and ambiguous subqueries`() {
        NoFromSubqueryRule().assertDiagnosticCount(
            """
            selectPlayers:
            SELECT id
            FROM player
            WHERE id IN (
              SELECT id
              FROM (SELECT id FROM player) AS nested_player
            );
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `ignores comments strings and quoted identifiers`() {
        NoFromSubqueryRule().assertDiagnosticCount(
            """
            selectLiteral:
            -- FROM (SELECT id FROM player)
            SELECT 'FROM (SELECT id FROM player)',
              "JOIN (SELECT id FROM player)",
              `FROM (SELECT id FROM player)`,
              [JOIN (SELECT id FROM player)]
            FROM player;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `allows comments between keywords and subquery parentheses`() {
        NoFromSubqueryRule().assertDiagnosticCount(
            """
            selectPlayers:
            SELECT ranked.id
            FROM /* ranked players */ (SELECT id FROM player) AS ranked;
            """.asSqlDelightFile(),
            1,
        )
    }
}
