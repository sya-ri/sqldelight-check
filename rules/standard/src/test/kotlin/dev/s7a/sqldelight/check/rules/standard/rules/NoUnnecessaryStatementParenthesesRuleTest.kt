package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.FixSafety
import kotlin.test.Test
import kotlin.test.assertEquals

class NoUnnecessaryStatementParenthesesRuleTest {
    @Test
    fun `reports parenthesized top level select statements`() {
        val content =
            """
            selectPlayers:
            (SELECT id, name
            FROM player);
            """.asSqlDelightFile()
        val diagnostics = NoUnnecessaryStatementParenthesesRule().diagnostics(content)

        assertEquals(1, diagnostics.size)
        assertEquals(FixSafety.Unsafe, diagnostics.single().fixes.single().safety)
        NoUnnecessaryStatementParenthesesRule().assertAllFixes(
            content,
            """
            selectPlayers:
            SELECT id, name
            FROM player;
            """.asSqlDelightFile(),
        )
    }

    @Test
    fun `reports parenthesized select statements in migration files`() {
        NoUnnecessaryStatementParenthesesRule().assertDiagnosticCount(
            """
            (SELECT id
            FROM player);
            """.asSqlDelightFile(),
            1,
            path = MIGRATION_SQM_PATH,
        )
    }

    @Test
    fun `accepts unparenthesized top level select statements`() {
        NoUnnecessaryStatementParenthesesRule().assertDiagnosticCount(
            """
            selectPlayers:
            SELECT id, name
            FROM player;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `ignores expression and subquery parentheses`() {
        NoUnnecessaryStatementParenthesesRule().assertDiagnosticCount(
            """
            selectPlayers:
            SELECT (score + 1) AS next_score
            FROM (SELECT id, score FROM player) AS ranked
            WHERE id IN (SELECT id FROM player);
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `ignores compound or ambiguous parenthesized statements`() {
        NoUnnecessaryStatementParenthesesRule().assertDiagnosticCount(
            """
            selectPlayers:
            (SELECT id FROM player)
            UNION ALL
            SELECT id FROM player_archive;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `ignores comments strings and quoted identifiers`() {
        NoUnnecessaryStatementParenthesesRule().assertDiagnosticCount(
            """
            selectLiteral:
            -- (SELECT id FROM player);
            SELECT '(SELECT id FROM player);',
              "(SELECT id FROM player);",
              `(SELECT id FROM player);`,
              [(SELECT id FROM player);]
            FROM player;
            """.asSqlDelightFile(),
            0,
        )
    }
}
