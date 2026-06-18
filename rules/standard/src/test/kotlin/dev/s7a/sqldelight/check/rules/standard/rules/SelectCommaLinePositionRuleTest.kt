package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.FixSafety
import kotlin.test.Test
import kotlin.test.assertEquals

class SelectCommaLinePositionRuleTest {
    @Test
    fun `reports leading commas in multiline select lists`() {
        val content =
            """
            selectPlayers:
            SELECT
              id
              , name
            FROM player;
            """.asSqlDelightFile()
        val diagnostics = SelectCommaLinePositionRule().diagnostics(content)

        assertEquals(1, diagnostics.size)
        assertEquals(FixSafety.Unsafe, diagnostics.single().fixes.single().safety)
        SelectCommaLinePositionRule().assertAllFixes(
            content,
            """
            selectPlayers:
            SELECT
              id,
               name
            FROM player;
            """.asSqlDelightFile(),
        )
    }

    @Test
    fun `accepts trailing commas in multiline select lists`() {
        SelectCommaLinePositionRule().assertDiagnosticCount(
            """
            selectPlayers:
            SELECT
              id,
              name
            FROM player;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `accepts single line select lists`() {
        SelectCommaLinePositionRule().assertDiagnosticCount(
            """
            selectPlayers:
            SELECT id, name FROM player;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `ignores commas in nested expressions`() {
        SelectCommaLinePositionRule().assertDiagnosticCount(
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
