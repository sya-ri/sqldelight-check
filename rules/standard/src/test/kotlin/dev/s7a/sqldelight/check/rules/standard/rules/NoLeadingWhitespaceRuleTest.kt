package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.FixSafety
import kotlin.test.Test
import kotlin.test.assertEquals

class NoLeadingWhitespaceRuleTest {
    @Test
    fun `reports safe fix for leading blank lines in sq schema file`() {
        val diagnostics =
            NoLeadingWhitespaceRule().diagnostics(
                """


                CREATE TABLE player (
                  id INTEGER NOT NULL PRIMARY KEY
                );
                """.asSqlDelightFile(),
            )

        assertEquals(1, diagnostics.size)
        assertEquals(FixSafety.Safe, diagnostics.single().fixes.single().safety)
        assertEquals("", diagnostics.single().fixes.single().edits.single().replacement)
    }

    @Test
    fun `reports safe fix for leading spaces before sq schema file content`() {
        val content =
            """
            <SP><SP>CREATE TABLE player (
              id INTEGER NOT NULL PRIMARY KEY
            );
            """.asSqlDelightFile().withSpaces()

        assertEquals("", NoLeadingWhitespaceRule().singleReplacement(content))
    }

    @Test
    fun `reports safe fix for leading tabs before sqm migration file content`() {
        val content =
            """
            <TAB><TAB>CREATE TABLE player (
              id INTEGER NOT NULL PRIMARY KEY
            );
            """.asSqlDelightFile().withTabs()

        assertEquals(
            "",
            NoLeadingWhitespaceRule().singleReplacement(content, path = MIGRATION_SQM_PATH),
        )
    }

    @Test
    fun `reports one diagnostic for mixed leading whitespace in sq raw string`() {
        val content =
            """

            <SP><TAB>CREATE TABLE player (
              id INTEGER NOT NULL PRIMARY KEY
            );
            """.asSqlDelightFile().withSpaces().withTabs()

        NoLeadingWhitespaceRule().assertDiagnosticCount(content, 1)
    }

    @Test
    fun `reports one diagnostic for mixed leading whitespace in sqm raw string`() {
        val content =
            """
            <TAB>
            <SP>CREATE TABLE player (
              id INTEGER NOT NULL PRIMARY KEY
            );
            """.asSqlDelightFile().withSpaces().withTabs()

        NoLeadingWhitespaceRule().assertDiagnosticCount(content, 1, path = MIGRATION_SQM_PATH)
    }

    @Test
    fun `accepts sq and sqm files that start with sql content`() {
        NoLeadingWhitespaceRule().assertDiagnosticCount(cleanPlayerSq, 0)
        NoLeadingWhitespaceRule().assertDiagnosticCount(cleanMigrationSqm, 0, path = MIGRATION_SQM_PATH)
    }

    @Test
    fun `accepts empty file`() {
        NoLeadingWhitespaceRule().assertDiagnosticCount("", 0)
    }

    @Test
    fun `reports safe fix for file with only whitespace`() {
        val content = "\n <TAB>\n".withTabs()

        assertEquals("", NoLeadingWhitespaceRule().singleReplacement(content))
    }
}
