package dev.s7a.sqldelight.check.rules.standard.rules

import kotlin.test.Test
import kotlin.test.assertEquals

class NoSelectStarRuleTest {
    @Test
    fun `reports wildcard result columns`() {
        val diagnostics =
            NoSelectStarRule().diagnostics(
                """
                selectPlayers:
                SELECT *
                FROM player;
                """.asSqlDelightFile(),
            )

        assertEquals(1, diagnostics.size)
        assertEquals(2, diagnostics.single().range?.start?.line)
    }

    @Test
    fun `accepts explicit result columns`() {
        NoSelectStarRule().assertDiagnosticCount(
            """
            selectPlayers:
            SELECT id, name, score
            FROM player;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `reports only top level wildcard result columns`() {
        NoSelectStarRule().assertDiagnosticCount(
            """
            selectPlayers:
            SELECT id, count(*) AS score_count, *
            FROM player;
            """.asSqlDelightFile(),
            1,
        )
    }
}
