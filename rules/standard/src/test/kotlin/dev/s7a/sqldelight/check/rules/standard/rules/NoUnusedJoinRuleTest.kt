package dev.s7a.sqldelight.check.rules.standard.rules

import kotlin.test.Test
import kotlin.test.assertEquals

class NoUnusedJoinRuleTest {
    @Test
    fun `reports joined aliases that are not referenced later`() {
        val diagnostics =
            NoUnusedJoinRule().diagnostics(
                """
                selectPlayers:
                SELECT p.id
                FROM player AS p
                JOIN team AS t ON p.team_id IS NOT NULL;
                """.asSqlDelightFile(),
            )

        assertEquals(1, diagnostics.size)
        assertEquals("JOIN source 't' is not referenced by later qualified column reads.", diagnostics.single().message)
    }

    @Test
    fun `accepts joined aliases referenced by join condition`() {
        NoUnusedJoinRule().assertDiagnosticCount(
            """
            selectPlayers:
            SELECT p.id
            FROM player AS p
            JOIN team AS t ON t.id = p.team_id;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `accepts joined table names referenced without aliases`() {
        NoUnusedJoinRule().assertDiagnosticCount(
            """
            selectPlayers:
            SELECT player.id
            FROM player
            JOIN team ON team.id = player.team_id;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `ignores references in comments and strings`() {
        NoUnusedJoinRule().assertDiagnosticCount(
            """
            selectPlayers:
            SELECT p.id, 't.id'
            FROM player AS p
            JOIN team AS t ON p.team_id IS NOT NULL
            -- t.id
            ;
            """.asSqlDelightFile(),
            1,
        )
    }
}
