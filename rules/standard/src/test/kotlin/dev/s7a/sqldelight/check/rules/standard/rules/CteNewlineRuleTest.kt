package dev.s7a.sqldelight.check.rules.standard.rules

import kotlin.test.Test

class CteNewlineRuleTest {
    @Test
    fun `reports cte definitions sharing one line`() {
        CteNewlineRule().assertDiagnosticCount(
            """
            selectRecent:
            WITH recent AS (SELECT id FROM event), active AS (SELECT id FROM player)
            SELECT *
            FROM recent;
            """.asSqlDelightFile(),
            1,
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
