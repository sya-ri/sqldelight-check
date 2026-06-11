package dev.s7a.sqldelight.check.rules.standard.rules

import kotlin.test.Test
import kotlin.test.assertEquals

class SelectModifierLinePositionRuleTest {
    @Test
    fun `reports select modifiers on a later line`() {
        val diagnostics =
            SelectModifierLinePositionRule().diagnostics(
                """
                selectNames:
                SELECT
                  DISTINCT name
                FROM player;
                selectAllNames:
                SELECT
                  ALL name
                FROM player;
                """.asSqlDelightFile(),
            )

        assertEquals(2, diagnostics.size)
        assertEquals(0, diagnostics.first().fixes.size)
    }

    @Test
    fun `reports select modifiers in migration files`() {
        SelectModifierLinePositionRule().assertDiagnosticCount(
            """
            INSERT INTO active_name(name)
            SELECT
              DISTINCT name
            FROM player;
            """.asSqlDelightFile(),
            1,
            path = MIGRATION_SQM_PATH,
        )
    }

    @Test
    fun `accepts modifiers on the select line`() {
        SelectModifierLinePositionRule().assertDiagnosticCount(
            """
            selectNames:
            SELECT DISTINCT name
            FROM player;
            selectAllNames:
            SELECT ALL name
            FROM player;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `ignores comments strings and quoted identifiers`() {
        SelectModifierLinePositionRule().assertDiagnosticCount(
            """
            selectLiteral:
            -- SELECT
            -- DISTINCT name
            SELECT 'DISTINCT', "ALL", `DISTINCT`, [ALL]
            FROM player;
            """.asSqlDelightFile(),
            0,
        )
    }
}
