package dev.s7a.sqldelight.check.rules.standard.rules

import kotlin.test.Test

class WhereConditionNewlineRuleTest {
    @Test
    fun `reports boolean operators sharing a line with predicates`() {
        WhereConditionNewlineRule().assertDiagnosticCount(
            """
            selectPlayers:
            SELECT id
            FROM player
            WHERE active = 1 AND deleted_at IS NULL
              AND team_id IS NOT NULL;
            """.asSqlDelightFile(),
            1,
        )
    }

    @Test
    fun `accepts boolean operators on their own lines`() {
        WhereConditionNewlineRule().assertDiagnosticCount(
            """
            selectPlayers:
            SELECT id
            FROM player
            WHERE active = 1
              AND deleted_at IS NULL
              AND team_id IS NOT NULL;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `ignores between and operator`() {
        WhereConditionNewlineRule().assertDiagnosticCount(
            """
            selectPlayers:
            SELECT id
            FROM player
            WHERE score BETWEEN 1 AND 10
              AND active = 1;
            """.asSqlDelightFile(),
            0,
        )
    }
}
