package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.FixSafety
import kotlin.test.Test
import kotlin.test.assertEquals

class NoSpaceBeforeSemicolonRuleTest {
    @Test
    fun `reports safe fix in sq named query`() {
        val diagnostics =
            NoSpaceBeforeSemicolonRule().diagnostics(
                """
                selectAll:
                SELECT id, name
                FROM player
                ORDER BY name ;
                """.asSqlDelightFile(),
            )

        assertEquals(1, diagnostics.size)
        assertEquals(FixSafety.Safe, diagnostics.single().fixes.single().safety)
        assertEquals("", diagnostics.single().fixes.single().edits.single().replacement)
        NoSpaceBeforeSemicolonRule().assertAllFixes(
            """
            selectAll:
            SELECT id, name
            FROM player
            ORDER BY name ;
            """.asSqlDelightFile(),
            """
            selectAll:
            SELECT id, name
            FROM player
            ORDER BY name;
            """.asSqlDelightFile(),
        )
    }

    @Test
    fun `reports tabs before semicolon in sqm migration`() {
        val content =
            """
            CREATE TABLE player (
              id INTEGER NOT NULL PRIMARY KEY
            )<TAB>;
            """.asSqlDelightFile().withTabs()

        assertEquals("", NoSpaceBeforeSemicolonRule().singleReplacement(content, path = MIGRATION_SQM_PATH))
        NoSpaceBeforeSemicolonRule().assertAllFixes(
            content,
            """
            CREATE TABLE player (
              id INTEGER NOT NULL PRIMARY KEY
            );
            """.asSqlDelightFile(),
            path = MIGRATION_SQM_PATH,
        )
    }

    @Test
    fun `accepts clean sq and sqm statement terminators`() {
        NoSpaceBeforeSemicolonRule().assertDiagnosticCount(cleanPlayerSq, 0)
        NoSpaceBeforeSemicolonRule().assertDiagnosticCount(cleanMigrationSqm, 0, path = MIGRATION_SQM_PATH)
    }

    @Test
    fun `ignores comments strings and quoted identifiers in sq files`() {
        val content =
            """
            selectLiteral:
            -- SELECT 1 ;
            SELECT '1 ;', "1 ;", `1 ;`, [1 ;];
            """.asSqlDelightFile()

        NoSpaceBeforeSemicolonRule().assertDiagnosticCount(content, 0)
    }
}
