package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.FixSafety
import kotlin.test.Test
import kotlin.test.assertEquals

class SelectTargetNewlineRuleTest {
    @Test
    fun `reports multiline select list with multiple targets on the select line`() {
        val content =
            """
            selectPlayers:
            SELECT id, name,
              age
            FROM player;
            """.asSqlDelightFile()
        val diagnostics = SelectTargetNewlineRule().diagnostics(content)

        assertEquals(1, diagnostics.size)
        assertEquals(FixSafety.Safe, diagnostics.single().fixes.single().safety)
        SelectTargetNewlineRule().assertAllFixes(
            content,
            """
            selectPlayers:
            SELECT
            id,
            name,
              age
            FROM player;
            """.asSqlDelightFile(),
        )
    }

    @Test
    fun `reports multiline select list with two targets on one line`() {
        SelectTargetNewlineRule().assertDiagnosticCount(
            """
            selectPlayers:
            SELECT
              id, name,
              age
            FROM player;
            """.asSqlDelightFile(),
            1,
        )
    }

    @Test
    fun `accepts one target per line`() {
        SelectTargetNewlineRule().assertDiagnosticCount(
            """
            selectPlayers:
            SELECT
              id,
              name,
              age
            FROM player;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `accepts single line select lists`() {
        SelectTargetNewlineRule().assertDiagnosticCount(
            """
            selectPlayers:
            SELECT id, name, age FROM player;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `ignores commas in nested expressions`() {
        SelectTargetNewlineRule().assertDiagnosticCount(
            """
            selectPlayers:
            SELECT
              COALESCE(name, 'missing') AS displayName,
              age
            FROM player;
            """.asSqlDelightFile(),
            0,
        )
    }
}
