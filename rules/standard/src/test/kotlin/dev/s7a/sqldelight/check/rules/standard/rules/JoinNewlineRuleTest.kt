package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.FixSafety
import kotlin.test.Test
import kotlin.test.assertEquals

class JoinNewlineRuleTest {
    @Test
    fun `reports joins sharing a line with from source`() {
        val content =
            """
            selectPlayers:
            SELECT player.id, team.name
            FROM player INNER JOIN team ON team.id = player.team_id
            WHERE player.active = 1;
            """.asSqlDelightFile()
        val diagnostics = JoinNewlineRule().diagnostics(content)

        assertEquals(1, diagnostics.size)
        assertEquals(FixSafety.Safe, diagnostics.single().fixes.single().safety)
        JoinNewlineRule().assertAllFixes(
            content,
            """
            selectPlayers:
            SELECT player.id, team.name
            FROM player
            INNER JOIN team ON team.id = player.team_id
            WHERE player.active = 1;
            """.asSqlDelightFile(),
        )
    }

    @Test
    fun `accepts joins on their own line`() {
        JoinNewlineRule().assertDiagnosticCount(
            """
            selectPlayers:
            SELECT player.id, team.name
            FROM player
            INNER JOIN team ON team.id = player.team_id
            WHERE player.active = 1;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `accepts single line statements`() {
        JoinNewlineRule().assertDiagnosticCount(
            """
            selectPlayers:
            SELECT player.id FROM player INNER JOIN team ON team.id = player.team_id;
            """.asSqlDelightFile(),
            0,
        )
    }
}
