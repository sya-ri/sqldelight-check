package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.FixSafety
import kotlin.test.Test
import kotlin.test.assertEquals

class NoTabIndentationRuleTest {
    @Test
    fun `reports safe fix in sq table indentation`() {
        val diagnostics =
            NoTabIndentationRule().diagnostics(
                """
                CREATE TABLE player (
                <TAB>id INTEGER NOT NULL PRIMARY KEY
                );
                """.asSqlDelightFile().withTabs(),
            )

        assertEquals(1, diagnostics.size)
        assertEquals(FixSafety.Safe, diagnostics.single().fixes.single().safety)
        assertEquals("    ", diagnostics.single().fixes.single().edits.single().replacement)
        NoTabIndentationRule().assertAllFixes(
            """
            CREATE TABLE player (
            <TAB>id INTEGER NOT NULL PRIMARY KEY
            );
            """.asSqlDelightFile().withTabs(),
            """
            CREATE TABLE player (
                id INTEGER NOT NULL PRIMARY KEY
            );
            """.asSqlDelightFile(),
        )
    }

    @Test
    fun `replaces each leading tab with four spaces`() {
        val content =
            """
            CREATE TABLE player (
            <TAB><TAB>name TEXT NOT NULL
            );
            """.asSqlDelightFile().withTabs()

        assertEquals("        ", NoTabIndentationRule().singleReplacement(content))
        NoTabIndentationRule().assertAllFixes(
            content,
            """
            CREATE TABLE player (
                    name TEXT NOT NULL
            );
            """.asSqlDelightFile(),
        )
    }

    @Test
    fun `accepts space indentation in sq and sqm files`() {
        NoTabIndentationRule().assertDiagnosticCount(cleanPlayerSq, 0)
        NoTabIndentationRule().assertDiagnosticCount(cleanMigrationSqm, 0, path = MIGRATION_SQM_PATH)
    }

    @Test
    fun `ignores tabs after indentation`() {
        NoTabIndentationRule().assertDiagnosticCount(
            """
            selectAll:
            SELECT<TAB>id
            FROM player;
            """.asSqlDelightFile().withTabs(),
            0,
        )
    }
}
