package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.FixSafety
import kotlin.test.Test
import kotlin.test.assertEquals

class NoSpaceBeforeDotRuleTest {
    @Test
    fun `reports safe fix in qualified column reference`() {
        val diagnostics =
            NoSpaceBeforeDotRule().diagnostics(
                """
                selectPlayer:
                SELECT player .id
                FROM player;
                """.asSqlDelightFile(),
            )

        assertEquals(1, diagnostics.size)
        assertEquals(FixSafety.Safe, diagnostics.single().fixes.single().safety)
        assertEquals("", diagnostics.single().fixes.single().edits.single().replacement)
    }

    @Test
    fun `accepts clean qualified column references`() {
        NoSpaceBeforeDotRule().assertDiagnosticCount(
            """
            selectPlayer:
            SELECT player.id
            FROM player;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `ignores comments strings and quoted identifiers`() {
        NoSpaceBeforeDotRule().assertDiagnosticCount(
            """
            selectLiteral:
            -- player .id
            SELECT 'player .id', "player .id", `player .id`, [player .id]
            FROM player;
            """.asSqlDelightFile(),
            0,
        )
    }
}
