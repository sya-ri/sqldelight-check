package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.FixSafety
import kotlin.test.Test
import kotlin.test.assertEquals

class NoSpaceAfterOpeningParenthesisRuleTest {
    @Test
    fun `reports safe fix in sq insert column list`() {
        val diagnostics =
            NoSpaceAfterOpeningParenthesisRule()
                .diagnostics(
                    """
                    insertPlayer:
                    INSERT INTO player( id, name, score)
                    VALUES (?, ?, ?);
                    """.asSqlDelightFile(),
                )

        assertEquals(1, diagnostics.size)
        assertEquals(FixSafety.Safe, diagnostics.single().fixes.single().safety)
        assertEquals("", diagnostics.single().fixes.single().edits.single().replacement)
    }

    @Test
    fun `reports tabs after opening parenthesis in sqm create table`() {
        val content =
            """
            CREATE TABLE player (<TAB>id INTEGER NOT NULL PRIMARY KEY
            );
            """.asSqlDelightFile().withTabs()

        assertEquals("", NoSpaceAfterOpeningParenthesisRule().singleReplacement(content, path = MIGRATION_SQM_PATH))
    }

    @Test
    fun `accepts clean sq and sqm parenthesis spacing`() {
        NoSpaceAfterOpeningParenthesisRule().assertDiagnosticCount(cleanPlayerSq, 0)
        NoSpaceAfterOpeningParenthesisRule().assertDiagnosticCount(cleanMigrationSqm, 0, path = MIGRATION_SQM_PATH)
    }

    @Test
    fun `accepts newline after opening parenthesis in create table`() {
        NoSpaceAfterOpeningParenthesisRule().assertDiagnosticCount(
            """
            CREATE TABLE player (
              id INTEGER NOT NULL PRIMARY KEY
            );
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `ignores comments strings and quoted identifiers in sq files`() {
        val content =
            """
            selectLiteral:
            -- COUNT( id)
            SELECT 'COUNT( id)', "COUNT( id)", `COUNT( id)`, [COUNT( id)];
            """.asSqlDelightFile()

        NoSpaceAfterOpeningParenthesisRule().assertDiagnosticCount(content, 0)
    }
}
