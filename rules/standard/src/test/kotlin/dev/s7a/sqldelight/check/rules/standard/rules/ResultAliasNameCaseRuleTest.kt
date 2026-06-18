package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.Severity
import kotlin.test.Test
import kotlin.test.assertEquals

class ResultAliasNameCaseRuleTest {
    @Test
    fun `reports result aliases that are not snake case`() {
        val content =
            """
            selectPlayer:
            SELECT player.name AS playerName
            FROM player;
            """.asSqlDelightFile()
        val diagnostics =
            ResultAliasNameCaseRule().diagnostics(
                content,
            )

        assertEquals(1, diagnostics.size)
        assertEquals(Severity.Info, diagnostics.single().severity)
        assertEquals(0, diagnostics.single().fixes.size)
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
