package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.FixSafety
import kotlin.test.Test
import kotlin.test.assertEquals

class CaseBranchNewlineRuleTest {
    @Test
    fun `reports multiline case branches sharing a line`() {
        val content =
            """
            selectPlayers:
            SELECT CASE WHEN active = 1 THEN 'active'
              ELSE 'inactive'
            END AS status
            FROM player;
            """.asSqlDelightFile()
        val diagnostics = CaseBranchNewlineRule().diagnostics(content)

        assertEquals(2, diagnostics.size)
        assertEquals(FixSafety.Safe, diagnostics.first().fixes.single().safety)
        CaseBranchNewlineRule().assertAllFixes(
            content,
            """
            selectPlayers:
            SELECT CASE
            WHEN active = 1
            THEN 'active'
              ELSE 'inactive'
            END AS status
            FROM player;
            """.asSqlDelightFile(),
        )
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

    @Test
    fun `uses dialect case expression block patterns`() {
        val diagnostics =
            CaseBranchNewlineRule().diagnostics(
                """
                selectPlayers:
                SELECT BEGIN ATOMIC WHEN active = 1 THEN 'active'
                  ELSE 'inactive'
                END AS status
                FROM player;
                """.asSqlDelightFile(),
                dialect = atomicCaseDialect,
            )

        assertEquals(2, diagnostics.size)
    }
}
