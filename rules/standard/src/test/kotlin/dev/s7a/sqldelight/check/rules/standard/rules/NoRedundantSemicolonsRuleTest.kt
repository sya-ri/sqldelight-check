package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.FixSafety
import kotlin.test.Test
import kotlin.test.assertEquals

class NoRedundantSemicolonsRuleTest {
    @Test
    fun `reports safe fix for directly consecutive semicolons in sq named query`() {
        val content =
            """
            selectAll:
            SELECT id
            FROM player
            ORDER BY name;;
            """.asSqlDelightFile()

        val diagnostics = NoRedundantSemicolonsRule().diagnostics(content)

        assertEquals(1, diagnostics.size)
        assertEquals(FixSafety.Safe, diagnostics.single().fixes.single().safety)
        assertEquals(
            """
            selectAll:
            SELECT id
            FROM player
            ORDER BY name;
            """.asSqlDelightFile(),
            NoRedundantSemicolonsRule().applySingleFix(content),
        )
    }

    @Test
    fun `reports safe fix for redundant semicolon after newline in sq named query`() {
        val content =
            """
            selectAll:
            SELECT id
            FROM player
            ORDER BY name;
            ;
            """.asSqlDelightFile()

        assertEquals(
            """
            selectAll:
            SELECT id
            FROM player
            ORDER BY name;
            """.asSqlDelightFile(),
            NoRedundantSemicolonsRule().applySingleFix(content),
        )
    }

    @Test
    fun `reports safe fix for whitespace separated redundant semicolons in migration`() {
        val content =
            """
            CREATE TABLE player (
              id INTEGER NOT NULL PRIMARY KEY
            );  <TAB>
              ;<SP><SP>
            ALTER TABLE player ADD COLUMN name TEXT;
            """.asSqlDelightFile().withTabs().withSpaces()

        val fixed = NoRedundantSemicolonsRule().applySingleFix(content, path = MIGRATION_SQM_PATH)

        assertEquals(
            """
            CREATE TABLE player (
              id INTEGER NOT NULL PRIMARY KEY
            );
            ALTER TABLE player ADD COLUMN name TEXT;
            """.asSqlDelightFile(),
            fixed,
        )
    }

    @Test
    fun `reports one diagnostic for a redundant semicolon run in migration`() {
        val content =
            """
            CREATE TABLE player (
              id INTEGER NOT NULL PRIMARY KEY
            ); ; ;
            """.asSqlDelightFile()

        NoRedundantSemicolonsRule().assertDiagnosticCount(content, 1, path = MIGRATION_SQM_PATH)
        assertEquals(
            """
            CREATE TABLE player (
              id INTEGER NOT NULL PRIMARY KEY
            );
            """.asSqlDelightFile(),
            NoRedundantSemicolonsRule().applySingleFix(content, path = MIGRATION_SQM_PATH),
        )
    }

    @Test
    fun `accepts clean sq and sqm statement terminators`() {
        NoRedundantSemicolonsRule().assertDiagnosticCount(cleanPlayerSq, 0)
        NoRedundantSemicolonsRule().assertDiagnosticCount(cleanMigrationSqm, 0, path = MIGRATION_SQM_PATH)
    }

    @Test
    fun `reports separate redundant semicolon runs independently in sq file`() {
        val content =
            """
            selectAll:
            SELECT id
            FROM player
            ORDER BY name;;

            selectById:
            SELECT id
            FROM player
            WHERE id = ?;
            ;
            """.asSqlDelightFile()

        NoRedundantSemicolonsRule().assertDiagnosticCount(content, 2)
    }

    @Test
    fun `ignores comments strings and quoted identifiers in sq files`() {
        val content =
            """
            selectLiteral:
            -- SELECT 1;;
            /* SELECT 1; ; */
            SELECT ';;', '; ;', ";;", "`; ;`", `;;`, [; ;];
            """.asSqlDelightFile()

        NoRedundantSemicolonsRule().assertDiagnosticCount(content, 0)
    }
}
