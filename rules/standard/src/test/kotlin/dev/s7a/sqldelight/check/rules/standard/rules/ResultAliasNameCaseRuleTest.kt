package dev.s7a.sqldelight.check.rules.standard.rules

import kotlin.test.Test
import kotlin.test.assertEquals

class ResultAliasNameCaseRuleTest {
    @Test
    fun `reports result aliases that are not snake case`() {
        val diagnostics =
            ResultAliasNameCaseRule().diagnostics(
                """
                selectPlayer:
                SELECT player.name AS playerName
                FROM player;
                """.asSqlDelightFile(),
            )

        assertEquals(1, diagnostics.size)
    }

    @Test
    fun `accepts snake case result aliases`() {
        ResultAliasNameCaseRule().assertDiagnosticCount(
            """
            selectPlayer:
            SELECT player.name AS player_name
            FROM player;
            """.asSqlDelightFile(),
            expected = 0,
        )
    }
}
