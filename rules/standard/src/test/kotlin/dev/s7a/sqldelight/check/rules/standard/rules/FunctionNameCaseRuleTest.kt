package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.FixSafety
import kotlin.test.Test
import kotlin.test.assertEquals

class FunctionNameCaseRuleTest {
    @Test
    fun `reports lowercase function names in sq named query`() {
        val diagnostics =
            FunctionNameCaseRule().diagnostics(
                """
                selectPlayerStats:
                SELECT count(*), coalesce(max(score), 0)
                FROM player;
                """.asSqlDelightFile(),
            )

        assertEquals(3, diagnostics.size)
        assertEquals(FixSafety.Unsafe, diagnostics.first().fixes.single().safety)
        assertEquals("COUNT", diagnostics.first().fixes.single().edits.single().replacement)
    }

    @Test
    fun `accepts uppercase function names`() {
        FunctionNameCaseRule().assertDiagnosticCount(
            """
            selectPlayerStats:
            SELECT COUNT(*), COALESCE(MAX(score), 0)
            FROM player;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `does not report function-like identifiers without call parenthesis`() {
        FunctionNameCaseRule().assertDiagnosticCount(
            """
            CREATE TABLE player (
              count INTEGER NOT NULL
            );
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `ignores comments strings and quoted identifiers`() {
        FunctionNameCaseRule().assertDiagnosticCount(
            """
            selectLiteral:
            -- count(*)
            SELECT 'count(*)', "count", `count`, [count]
            FROM player;
            """.asSqlDelightFile(),
            0,
        )
    }
}
