package dev.s7a.sqldelight.check.rules.standard.rules

import kotlin.test.Test

class JoinNewlineRuleTest {
    @Test
    fun `reports joins sharing a line with from source`() {
        JoinNewlineRule().assertDiagnosticCount(
            """
            selectPlayers:
            SELECT player.id, team.name
            FROM player INNER JOIN team ON team.id = player.team_id
            WHERE player.active = 1;
            """.asSqlDelightFile(),
            1,
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
