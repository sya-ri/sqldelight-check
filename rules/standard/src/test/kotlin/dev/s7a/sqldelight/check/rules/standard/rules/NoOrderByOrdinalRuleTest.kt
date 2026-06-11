package dev.s7a.sqldelight.check.rules.standard.rules

import kotlin.test.Test
import kotlin.test.assertEquals

class NoOrderByOrdinalRuleTest {
    @Test
    fun `reports order by ordinal references`() {
        val diagnostics =
            NoOrderByOrdinalRule().diagnostics(
                """
                listPlayers:
                SELECT id, name
                FROM player
                ORDER BY 2 DESC NULLS LAST;
                """.asSqlDelightFile(),
            )

        assertEquals(1, diagnostics.size)
        assertEquals("ORDER BY should reference columns by name instead of ordinal.", diagnostics.single().message)
    }

    @Test
    fun `reports group by ordinal references`() {
        NoOrderByOrdinalRule().assertDiagnosticCount(
            """
            playerCounts:
            SELECT team_id, COUNT(*)
            FROM player
            GROUP BY 1;
            """.asSqlDelightFile(),
            expected = 1,
        )
    }

    @Test
    fun `accepts named references`() {
        NoOrderByOrdinalRule().assertDiagnosticCount(
            """
            playerCounts:
            SELECT team_id, COUNT(*) AS playerCount
            FROM player
            GROUP BY team_id
            ORDER BY playerCount DESC;
            """.asSqlDelightFile(),
            expected = 0,
        )
    }
}
