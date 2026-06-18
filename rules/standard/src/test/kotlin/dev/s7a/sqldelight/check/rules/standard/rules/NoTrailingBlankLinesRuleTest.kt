package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.FixSafety
import kotlin.test.Test
import kotlin.test.assertEquals

class NoTrailingBlankLinesRuleTest {
    @Test
    fun `reports safe fix after sq named query file content`() {
        val diagnostics =
            NoTrailingBlankLinesRule().diagnostics(
                """
                CREATE TABLE player (
                  id INTEGER NOT NULL PRIMARY KEY
                );

                selectAll:
                SELECT id
                FROM player;
                """.asSqlDelightFile() + "\n",
            )

        assertEquals(1, diagnostics.size)
        assertEquals(FixSafety.Safe, diagnostics.single().fixes.single().safety)
        assertEquals("", diagnostics.single().fixes.single().edits.single().replacement)
        NoTrailingBlankLinesRule().assertAllFixes(
            """
            CREATE TABLE player (
              id INTEGER NOT NULL PRIMARY KEY
            );

            selectAll:
            SELECT id
            FROM player;
            """.asSqlDelightFile() + "\n",
            """
            CREATE TABLE player (
              id INTEGER NOT NULL PRIMARY KEY
            );

            selectAll:
            SELECT id
            FROM player;
            """.asSqlDelightFile(),
        )
    }

    @Test
    fun `accepts single final newline in sq and sqm files`() {
        NoTrailingBlankLinesRule().assertDiagnosticCount(cleanPlayerSq, 0, path = PLAYER_SQ_PATH)
        NoTrailingBlankLinesRule().assertDiagnosticCount(cleanMigrationSqm, 0, path = MIGRATION_SQM_PATH)
    }

    @Test
    fun `accepts sq file without final newline`() {
        NoTrailingBlankLinesRule().assertDiagnosticCount(cleanPlayerSq.removeSuffix("\n"), 0)
    }

    @Test
    fun `accepts file with only blank lines`() {
        NoTrailingBlankLinesRule().assertDiagnosticCount("\n\n", 0)
    }
}
