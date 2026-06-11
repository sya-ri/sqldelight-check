package dev.s7a.sqldelight.check.rules.standard.rules

import kotlin.test.Test
import kotlin.test.assertEquals

class OperatorLinePositionRuleTest {
    @Test
    fun `reports line-leading comparison and binary operators in sq queries`() {
        val diagnostics =
            OperatorLinePositionRule().diagnostics(
                """
                selectMatches:
                SELECT id, name
                FROM player
                WHERE id
                  = ?
                  AND score
                  + bonus > 10
                  AND name
                  || suffix = ?;
                """.asSqlDelightFile(),
            )

        assertEquals(3, diagnostics.size)
        assertEquals(0, diagnostics.first().fixes.size)
    }

    @Test
    fun `reports operators in migration files`() {
        OperatorLinePositionRule().assertDiagnosticCount(
            """
            UPDATE player
            SET score = score
              + 1
            WHERE id = 1;
            """.asSqlDelightFile(),
            1,
            path = MIGRATION_SQM_PATH,
        )
    }

    @Test
    fun `accepts trailing operators and single line sql`() {
        OperatorLinePositionRule().assertDiagnosticCount(
            """
            selectMatches:
            SELECT id, name
            FROM player
            WHERE id =
              ?
              AND score +
              bonus > 10
              AND name || suffix = ?;
            """.asSqlDelightFile(),
            0,
        )
        OperatorLinePositionRule().assertDiagnosticCount("SELECT id FROM player WHERE id = ?;", 0)
    }

    @Test
    fun `accepts line-leading wildcard and unary signs`() {
        OperatorLinePositionRule().assertDiagnosticCount(
            """
            selectAll:
            SELECT
              *
            FROM player
            WHERE score IN (
              -1,
              +1
            );
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `ignores comments strings and quoted identifiers`() {
        OperatorLinePositionRule().assertDiagnosticCount(
            """
            selectLiteral:
            -- = 1
            SELECT '= 1', "+ 1", `* value`, [/ value]
            FROM player;
            """.asSqlDelightFile(),
            0,
        )
    }
}
