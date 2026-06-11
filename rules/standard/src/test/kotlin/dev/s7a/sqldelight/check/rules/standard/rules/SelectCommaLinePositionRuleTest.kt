package dev.s7a.sqldelight.check.rules.standard.rules

import kotlin.test.Test
import kotlin.test.assertEquals

class SelectCommaLinePositionRuleTest {
    @Test
    fun `reports leading commas in multiline select lists`() {
        val diagnostics =
            SelectCommaLinePositionRule().diagnostics(
                """
                selectPlayers:
                SELECT
                  id
                  , name
                FROM player;
                """.asSqlDelightFile(),
            )

        assertEquals(1, diagnostics.size)
        assertEquals(0, diagnostics.single().fixes.size)
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
