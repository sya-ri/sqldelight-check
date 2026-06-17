package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.FixSafety
import kotlin.test.Test
import kotlin.test.assertEquals

class NoSpaceBeforeClosingParenthesisRuleTest {
    @Test
    fun `reports safe fix in sq insert column list`() {
        val diagnostics =
            NoSpaceBeforeClosingParenthesisRule()
                .diagnostics(
                    """
                    insertPlayer:
                    INSERT INTO player(id, name, score )
                    VALUES (?, ?, ?);
                    """.asSqlDelightFile(),
                )

        assertEquals(1, diagnostics.size)
        assertEquals(FixSafety.Safe, diagnostics.single().fixes.single().safety)
        assertEquals("", diagnostics.single().fixes.single().edits.single().replacement)
        NoSpaceBeforeClosingParenthesisRule().assertAllFixes(
            """
            insertPlayer:
            INSERT INTO player(id, name, score )
            VALUES (?, ?, ?);
            """.asSqlDelightFile(),
            """
            insertPlayer:
            INSERT INTO player(id, name, score)
            VALUES (?, ?, ?);
            """.asSqlDelightFile(),
        )
    }

    @Test
    fun `reports tabs before closing parenthesis in sqm create table`() {
        val content =
            """
            CREATE TABLE player (
              id INTEGER NOT NULL PRIMARY KEY<TAB>);
            """.asSqlDelightFile().withTabs()

        assertEquals("", NoSpaceBeforeClosingParenthesisRule().singleReplacement(content, path = MIGRATION_SQM_PATH))
        NoSpaceBeforeClosingParenthesisRule().assertAllFixes(
            content,
            """
            CREATE TABLE player (
              id INTEGER NOT NULL PRIMARY KEY);
            """.asSqlDelightFile(),
            path = MIGRATION_SQM_PATH,
        )
    }

    @Test
    fun `accepts clean sq and sqm parenthesis spacing`() {
        NoSpaceBeforeClosingParenthesisRule().assertDiagnosticCount(cleanPlayerSq, 0)
        NoSpaceBeforeClosingParenthesisRule().assertDiagnosticCount(cleanMigrationSqm, 0, path = MIGRATION_SQM_PATH)
    }

    @Test
    fun `accepts newline before closing parenthesis in create table`() {
        NoSpaceBeforeClosingParenthesisRule().assertDiagnosticCount(cleanMigrationSqm, 0, path = MIGRATION_SQM_PATH)
    }

    @Test
    fun `accepts indented multiline check constraint closing parenthesis`() {
        val content =
            """
            CREATE TABLE sample (
              id INTEGER NOT NULL PRIMARY KEY,
              reason TEXT NOT NULL,
              detail TEXT,
              CHECK (
                (reason = 'OTHER' AND detail IS NOT NULL AND detail != '')
                  OR (reason != 'OTHER' AND detail IS NULL)
              )
            );
            """.asSqlDelightFile()

        NoSpaceBeforeClosingParenthesisRule().assertDiagnosticCount(content, 0, path = MIGRATION_SQM_PATH)
    }

    @Test
    fun `ignores comments strings and quoted identifiers in sq files`() {
        val content =
            """
            selectLiteral:
            -- COUNT(id )
            SELECT 'COUNT(id )', "COUNT(id )", `COUNT(id )`, [COUNT(id )];
            """.asSqlDelightFile()

        NoSpaceBeforeClosingParenthesisRule().assertDiagnosticCount(content, 0)
    }
}
