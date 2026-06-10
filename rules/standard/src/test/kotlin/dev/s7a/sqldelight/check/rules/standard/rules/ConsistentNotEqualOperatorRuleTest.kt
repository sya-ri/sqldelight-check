package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.FixSafety
import kotlin.test.Test
import kotlin.test.assertEquals

class ConsistentNotEqualOperatorRuleTest {
    @Test
    fun `reports unsafe fix for mixed not equal operators`() {
        val diagnostics =
            ConsistentNotEqualOperatorRule().diagnostics(
                """
                selectMismatchedPlayers:
                SELECT id, name
                FROM player
                WHERE name != 'admin' AND status <> 'deleted';
                """.asSqlDelightFile(),
            )

        assertEquals(1, diagnostics.size)
        assertEquals(FixSafety.Unsafe, diagnostics.single().fixes.single().safety)
        assertEquals("!=", diagnostics.single().fixes.single().edits.single().replacement)
    }

    @Test
    fun `uses the first not equal style as the file convention`() {
        val diagnostics =
            ConsistentNotEqualOperatorRule().diagnostics(
                """
                selectMismatchedPlayers:
                SELECT id, name
                FROM player
                WHERE name <> 'admin' AND status != 'deleted';
                """.asSqlDelightFile(),
            )

        assertEquals(1, diagnostics.size)
        assertEquals("<>", diagnostics.single().fixes.single().edits.single().replacement)
    }

    @Test
    fun `reports mixed not equal operators in migration files`() {
        val diagnostics =
            ConsistentNotEqualOperatorRule().diagnostics(
                """
                DELETE FROM player
                WHERE name != 'admin' AND status <> 'deleted';
                """.asSqlDelightFile(),
                path = MIGRATION_SQM_PATH,
            )

        assertEquals(1, diagnostics.size)
    }

    @Test
    fun `accepts consistent not equal operators`() {
        ConsistentNotEqualOperatorRule().assertDiagnosticCount(
            """
            selectMismatchedPlayers:
            SELECT id, name
            FROM player
            WHERE name != 'admin' AND status != 'deleted';
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `ignores comments strings and quoted identifiers`() {
        ConsistentNotEqualOperatorRule().assertDiagnosticCount(
            """
            selectLiteral:
            -- name != 'admin' AND status <> 'deleted'
            SELECT '!= <>', "!= <>", `!= <>`, [!= <>]
            FROM player;
            """.asSqlDelightFile(),
            0,
        )
    }
}
