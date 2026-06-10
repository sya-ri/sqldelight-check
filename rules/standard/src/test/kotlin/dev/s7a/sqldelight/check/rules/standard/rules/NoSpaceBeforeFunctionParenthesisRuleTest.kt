package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.FixSafety
import kotlin.test.Test
import kotlin.test.assertEquals

class NoSpaceBeforeFunctionParenthesisRuleTest {
    @Test
    fun `reports safe fix before function parenthesis`() {
        val diagnostics =
            NoSpaceBeforeFunctionParenthesisRule().diagnostics(
                """
                selectPlayerStats:
                SELECT COUNT (*), COALESCE (MAX(score), 0)
                FROM player;
                """.asSqlDelightFile(),
            )

        assertEquals(2, diagnostics.size)
        assertEquals(FixSafety.Safe, diagnostics.first().fixes.single().safety)
        assertEquals("", diagnostics.first().fixes.single().edits.single().replacement)
    }

    @Test
    fun `accepts function parenthesis without preceding space`() {
        NoSpaceBeforeFunctionParenthesisRule().assertDiagnosticCount(
            """
            selectPlayerStats:
            SELECT COUNT(*), COALESCE(MAX(score), 0)
            FROM player;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `does not report non function parenthesis`() {
        NoSpaceBeforeFunctionParenthesisRule().assertDiagnosticCount(
            """
            selectWrapped:
            SELECT *
            FROM (SELECT id FROM player) AS nested_player;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `ignores comments strings and quoted identifiers`() {
        NoSpaceBeforeFunctionParenthesisRule().assertDiagnosticCount(
            """
            selectLiteral:
            -- COUNT (*)
            SELECT 'COUNT (*)', "COUNT", `COUNT`, [COUNT]
            FROM player;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `reports functions in migration files`() {
        val diagnostics =
            NoSpaceBeforeFunctionParenthesisRule().diagnostics(
                """
                INSERT INTO player(id, name, score)
                SELECT id, COALESCE (name, 'unknown'), ABS (score)
                FROM legacy_player;
                """.asSqlDelightFile(),
                path = MIGRATION_SQM_PATH,
            )

        assertEquals(2, diagnostics.size)
    }
}
