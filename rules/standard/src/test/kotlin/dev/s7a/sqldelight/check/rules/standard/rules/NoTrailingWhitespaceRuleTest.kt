package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.FixSafety
import kotlin.test.Test
import kotlin.test.assertEquals

class NoTrailingWhitespaceRuleTest {
    @Test
    fun `reports safe fix in sq named query`() {
        val diagnostics =
            NoTrailingWhitespaceRule().diagnostics(
                """
                selectAll:
                SELECT id, name
                FROM player
                ORDER BY name;<SP><SP>
                """.asSqlDelightFile().withSpaces(),
            )

        assertEquals(1, diagnostics.size)
        assertEquals(FixSafety.Safe, diagnostics.single().fixes.single().safety)
        assertEquals("", diagnostics.single().fixes.single().edits.single().replacement)
        NoTrailingWhitespaceRule().assertAllFixes(
            """
            selectAll:
            SELECT id, name
            FROM player
            ORDER BY name;<SP><SP>
            """.asSqlDelightFile().withSpaces(),
            """
            selectAll:
            SELECT id, name
            FROM player
            ORDER BY name;
            """.asSqlDelightFile(),
        )
    }

    @Test
    fun `reports trailing tabs in sqm migration`() {
        val content =
            """
            CREATE TABLE player (
              id INTEGER NOT NULL PRIMARY KEY
            );<TAB>
            """.asSqlDelightFile().withTabs().withSpaces()

        assertEquals("", NoTrailingWhitespaceRule().singleReplacement(content, path = MIGRATION_SQM_PATH))
        NoTrailingWhitespaceRule().assertAllFixes(
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
    fun `reports each affected line in sq file`() {
        val content =
            """
            CREATE TABLE player (<SP><SP>
              id INTEGER NOT NULL PRIMARY KEY
            );

            selectAll:<SP><SP>
            SELECT id
            FROM player;
            """.asSqlDelightFile().withSpaces()

        NoTrailingWhitespaceRule().assertDiagnosticCount(content, 2)
    }

    @Test
    fun `accepts clean sq and sqm files`() {
        NoTrailingWhitespaceRule().assertDiagnosticCount(cleanPlayerSq, 0)
        NoTrailingWhitespaceRule().assertDiagnosticCount(cleanMigrationSqm, 0, path = MIGRATION_SQM_PATH)
    }
}
