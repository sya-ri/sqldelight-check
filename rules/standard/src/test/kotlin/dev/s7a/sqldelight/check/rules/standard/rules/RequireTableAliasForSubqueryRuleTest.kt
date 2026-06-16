package dev.s7a.sqldelight.check.rules.standard.rules

import kotlin.test.Test
import kotlin.test.assertEquals

class RequireTableAliasForSubqueryRuleTest {
    @Test
    fun `reports from subqueries without aliases`() {
        val diagnostics =
            RequireTableAliasForSubqueryRule().diagnostics(
                """
                selectPlayers:
                SELECT id
                FROM (SELECT id FROM player);
                """.asSqlDelightFile(),
            )

        assertEquals(1, diagnostics.size)
        assertEquals("FROM and JOIN subqueries should have a table alias.", diagnostics.single().message)
        assertEquals(0, diagnostics.single().fixes.size)
    }

    @Test
    fun `reports join subqueries without aliases`() {
        RequireTableAliasForSubqueryRule().assertDiagnosticCount(
            """
            selectPlayers:
            SELECT player.id
            FROM player
            JOIN (SELECT player_id FROM score)
              ON player_id = player.id;
            """.asSqlDelightFile(),
            1,
        )
    }

    @Test
    fun `does not treat table reference boundaries as aliases`() {
        RequireTableAliasForSubqueryRule().assertDiagnosticCount(
            """
            selectPlayers:
            SELECT player.id
            FROM (SELECT id FROM player)
            JOIN score ON score.player_id = player.id;
            """.asSqlDelightFile(),
            1,
        )
        RequireTableAliasForSubqueryRule().assertDiagnosticCount(
            """
            selectPlayers:
            SELECT player.id
            FROM (SELECT id FROM player),
                 score;
            """.asSqlDelightFile(),
            1,
        )
    }

    @Test
    fun `accepts explicit and implicit subquery aliases`() {
        RequireTableAliasForSubqueryRule().assertDiagnosticCount(
            """
            selectPlayers:
            SELECT ranked.id, recent.player_id
            FROM (SELECT id FROM player) AS ranked
            JOIN (SELECT player_id FROM score) recent
              ON recent.player_id = ranked.id;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `ignores nested and ambiguous subqueries`() {
        RequireTableAliasForSubqueryRule().assertDiagnosticCount(
            """
            selectPlayers:
            SELECT id
            FROM player
            WHERE id IN (
              SELECT id
              FROM (SELECT id FROM player)
            );
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `uses dialect subquery block patterns`() {
        RequireTableAliasForSubqueryRule().assertDiagnosticCount(
            """
            selectPlayers:
            SELECT id
            FROM {SELECT id FROM player};
            """.asSqlDelightFile(),
            1,
            dialect = braceSubqueryDialect,
        )
        RequireTableAliasForSubqueryRule().assertDiagnosticCount(
            """
            selectPlayers:
            SELECT ranked.id
            FROM {SELECT id FROM player} AS ranked;
            """.asSqlDelightFile(),
            0,
            dialect = braceSubqueryDialect,
        )
    }
}
