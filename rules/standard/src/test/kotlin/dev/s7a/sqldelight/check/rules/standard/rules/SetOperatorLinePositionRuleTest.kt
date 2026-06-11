package dev.s7a.sqldelight.check.rules.standard.rules

import kotlin.test.Test
import kotlin.test.assertEquals

class SetOperatorLinePositionRuleTest {
    @Test
    fun `reports set operators after other code on multiline queries`() {
        val diagnostics =
            SetOperatorLinePositionRule().diagnostics(
                """
                selectNames:
                SELECT name FROM player UNION
                SELECT name FROM coach EXCEPT
                SELECT name FROM archived_player INTERSECT
                SELECT name FROM sponsor;
                """.asSqlDelightFile(),
            )

        assertEquals(3, diagnostics.size)
        assertEquals(0, diagnostics.first().fixes.size)
    }

    @Test
    fun `reports set operators in migration files`() {
        SetOperatorLinePositionRule().assertDiagnosticCount(
            """
            INSERT INTO active_name(name)
            SELECT name FROM player UNION
            SELECT name FROM coach;
            """.asSqlDelightFile(),
            1,
            path = MIGRATION_SQM_PATH,
        )
    }

    @Test
    fun `accepts set operators at beginning of line after indentation`() {
        SetOperatorLinePositionRule().assertDiagnosticCount(
            """
            selectNames:
            SELECT name FROM player
            UNION ALL
            SELECT name FROM coach
              EXCEPT
            SELECT name FROM archived_player
            <TAB>INTERSECT
            SELECT name FROM sponsor;
            """.asSqlDelightFile().withTabs(),
            0,
        )
    }

    @Test
    fun `accepts single line sql`() {
        SetOperatorLinePositionRule().assertDiagnosticCount(
            "SELECT name FROM player UNION SELECT name FROM coach;",
            0,
        )
    }

    @Test
    fun `ignores comments strings and quoted identifiers`() {
        SetOperatorLinePositionRule().assertDiagnosticCount(
            """
            selectLiteral:
            -- SELECT name FROM player UNION SELECT name FROM coach
            SELECT 'UNION', "EXCEPT", `INTERSECT`, [UNION]
            FROM player;
            """.asSqlDelightFile(),
            0,
        )
    }
}
