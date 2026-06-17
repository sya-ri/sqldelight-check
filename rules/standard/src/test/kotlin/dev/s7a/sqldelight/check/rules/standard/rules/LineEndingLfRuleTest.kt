package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.FixSafety
import kotlin.test.Test
import kotlin.test.assertEquals

class LineEndingLfRuleTest {
    @Test
    fun `reports safe fix for sq file with crlf`() {
        val content =
            """
            CREATE TABLE player (
              id INTEGER NOT NULL PRIMARY KEY
            );

            selectAll:
            SELECT id
            FROM player;
            """.asSqlDelightFile().replaceFirst("\n", "\r\n")
        val diagnostics = LineEndingLfRule().diagnostics(content)

        assertEquals(1, diagnostics.size)
        assertEquals(FixSafety.Safe, diagnostics.single().fixes.single().safety)
        assertEquals("\n", diagnostics.single().fixes.single().edits.single().replacement)
        LineEndingLfRule().assertAllFixes(
            content,
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
    fun `reports lone carriage return in sqm migration file`() {
        val content =
            """
            CREATE TABLE player (
              id INTEGER NOT NULL PRIMARY KEY
            );
            """.asSqlDelightFile().replaceFirst("\n", "\r")
        val diagnostics = LineEndingLfRule().diagnostics(content, path = MIGRATION_SQM_PATH)

        assertEquals(1, diagnostics.size)
        assertEquals("\n", diagnostics.single().fixes.single().edits.single().replacement)
        LineEndingLfRule().assertAllFixes(
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
    fun `reports every non lf line ending in named query file`() {
        LineEndingLfRule().assertDiagnosticCount(cleanPlayerSq.replace("\n", "\r\n"), cleanPlayerSq.count { character -> character == '\n' })
    }

    @Test
    fun `accepts lf line endings in sq and sqm files`() {
        LineEndingLfRule().assertDiagnosticCount(cleanPlayerSq, 0, path = PLAYER_SQ_PATH)
        LineEndingLfRule().assertDiagnosticCount(cleanMigrationSqm, 0, path = MIGRATION_SQM_PATH)
    }
}
