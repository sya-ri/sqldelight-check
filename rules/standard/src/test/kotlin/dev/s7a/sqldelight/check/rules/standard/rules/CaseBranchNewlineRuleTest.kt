package dev.s7a.sqldelight.check.rules.standard.rules

import kotlin.test.Test
import kotlin.test.assertEquals

class CaseBranchNewlineRuleTest {
    @Test
    fun `reports multiline case branches sharing a line`() {
        val diagnostics =
            CaseBranchNewlineRule().diagnostics(
                """
                selectPlayers:
                SELECT CASE WHEN active = 1 THEN 'active'
                  ELSE 'inactive'
                END AS status
                FROM player;
                """.asSqlDelightFile(),
            )

        assertEquals(2, diagnostics.size)
        assertEquals(0, diagnostics.first().fixes.size)
    }

    @Test
    fun `accepts multiline case branches on their own lines`() {
        CaseBranchNewlineRule().assertDiagnosticCount(
            """
            selectPlayers:
            SELECT CASE
              WHEN active = 1
              THEN 'active'
              ELSE 'inactive'
            END AS status
            FROM player;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `accepts single line case expressions`() {
        CaseBranchNewlineRule().assertDiagnosticCount(
            """
            selectPlayers:
            SELECT CASE WHEN active = 1 THEN 'active' ELSE 'inactive' END AS status FROM player;
            """.asSqlDelightFile(),
            0,
        )
    }
}
