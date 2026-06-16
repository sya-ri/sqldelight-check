package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.FixSafety
import kotlin.test.Test
import kotlin.test.assertEquals

class NoSpaceAfterDotRuleTest {
    @Test
    fun `reports safe fix in qualified column reference`() {
        val diagnostics =
            NoSpaceAfterDotRule().diagnostics(
                """
                selectPlayer:
                SELECT player. id
                FROM player;
                """.asSqlDelightFile(),
            )

        assertEquals(1, diagnostics.size)
        assertEquals(FixSafety.Safe, diagnostics.single().fixes.single().safety)
        assertEquals("", diagnostics.single().fixes.single().edits.single().replacement)
        NoSpaceAfterDotRule().assertAllFixes(
            """
            selectPlayer:
            SELECT player. id
            FROM player;
            """.asSqlDelightFile(),
            """
            selectPlayer:
            SELECT player.id
            FROM player;
            """.asSqlDelightFile(),
        )
    }

    @Test
    fun `reports tab after dot`() {
        val content =
            """
            selectPlayer:
            SELECT player.<TAB>id
            FROM player;
            """.asSqlDelightFile().withTabs()

        assertEquals("", NoSpaceAfterDotRule().singleReplacement(content))
        NoSpaceAfterDotRule().assertAllFixes(
            content,
            """
            selectPlayer:
            SELECT player.id
            FROM player;
            """.asSqlDelightFile(),
        )
    }

    @Test
    fun `accepts clean qualified column references`() {
        NoSpaceAfterDotRule().assertDiagnosticCount(
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
        NoSpaceAfterDotRule().assertDiagnosticCount(
            """
            selectLiteral:
            -- player. id
            SELECT 'player. id', "player. id", `player. id`, [player. id]
            FROM player;
            """.asSqlDelightFile(),
            0,
        )
    }
}
