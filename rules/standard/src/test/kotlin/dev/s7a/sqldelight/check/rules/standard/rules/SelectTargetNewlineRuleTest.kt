package dev.s7a.sqldelight.check.rules.standard.rules

import kotlin.test.Test
import kotlin.test.assertEquals

class SelectTargetNewlineRuleTest {
    @Test
    fun `reports multiline select list with multiple targets on the select line`() {
        val diagnostics =
            SelectTargetNewlineRule().diagnostics(
                """
                selectPlayers:
                SELECT id, name,
                  age
                FROM player;
                """.asSqlDelightFile(),
            )

        assertEquals(1, diagnostics.size)
        assertEquals(0, diagnostics.single().fixes.size)
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
