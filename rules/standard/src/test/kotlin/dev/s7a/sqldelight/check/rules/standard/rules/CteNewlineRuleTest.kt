package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.FixSafety
import kotlin.test.Test
import kotlin.test.assertEquals

class CteNewlineRuleTest {
    @Test
    fun `reports cte definitions sharing one line`() {
        val content =
            """
            selectRecent:
            WITH recent AS (SELECT id FROM event), active AS (SELECT id FROM player)
            SELECT *
            FROM recent;
            """.asSqlDelightFile()
        val diagnostics = CteNewlineRule().diagnostics(content)

        assertEquals(1, diagnostics.size)
        assertEquals(FixSafety.Safe, diagnostics.single().fixes.single().safety)
        CteNewlineRule().assertAllFixes(
            content,
            """
            selectRecent:
            WITH
            recent AS (SELECT id FROM event),
            active AS (SELECT id FROM player)
            SELECT *
            FROM recent;
            """.asSqlDelightFile(),
        )
    }

    @Test
    fun `accepts cte definitions on their own lines`() {
        CteNewlineRule().assertDiagnosticCount(
            """
            selectRecent:
            WITH
              recent AS (SELECT id FROM event),
              active AS (SELECT id FROM player)
            SELECT *
            FROM recent;
            """.asSqlDelightFile(),
            0,
        )
    }
}
