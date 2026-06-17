package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.FixSafety
import kotlin.test.Test
import kotlin.test.assertEquals

class SpaceAfterBlockCommentStartRuleTest {
    @Test
    fun `reports safe fix after block comment start`() {
        val diagnostics =
            SpaceAfterBlockCommentStartRule().diagnostics(
                """
                /*Player table.*/
                CREATE TABLE player (
                  id INTEGER NOT NULL PRIMARY KEY
                );
                """.asSqlDelightFile(),
            )

        assertEquals(1, diagnostics.size)
        assertEquals(FixSafety.Safe, diagnostics.single().fixes.single().safety)
        assertEquals(" ", diagnostics.single().fixes.single().edits.single().replacement)
        SpaceAfterBlockCommentStartRule().assertAllFixes(
            """
            /*Player table.*/
            CREATE TABLE player (
              id INTEGER NOT NULL PRIMARY KEY
            );
            """.asSqlDelightFile(),
            """
            /* Player table.*/
            CREATE TABLE player (
              id INTEGER NOT NULL PRIMARY KEY
            );
            """.asSqlDelightFile(),
        )
    }

    @Test
    fun `accepts spaced empty and hint-like block comments`() {
        SpaceAfterBlockCommentStartRule().assertDiagnosticCount(
            """
            /* Player table. */
            /**/
            /*+ sqlite hint-like comment */
            CREATE TABLE player (
              id INTEGER NOT NULL PRIMARY KEY
            );
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `reports block comments in named queries and migrations`() {
        val queryDiagnostics =
            SpaceAfterBlockCommentStartRule().diagnostics(
                """
                selectActive:
                SELECT id, name
                FROM player
                WHERE active = 1 /*active players only */;
                """.asSqlDelightFile(),
            )
        val migrationDiagnostics =
            SpaceAfterBlockCommentStartRule().diagnostics(
                """
                /*backfill existing rows */
                UPDATE player
                SET active = 1;
                """.asSqlDelightFile(),
                path = MIGRATION_SQM_PATH,
            )

        assertEquals(1, queryDiagnostics.size)
        assertEquals(1, migrationDiagnostics.size)
    }

    @Test
    fun `ignores block comment marker inside strings and quoted identifiers`() {
        SpaceAfterBlockCommentStartRule().assertDiagnosticCount(
            """
            selectLiteral:
            SELECT '/*comment*/', "/*comment*/", `/*comment*/`, [/*comment*/]
            FROM player;
            """.asSqlDelightFile(),
            0,
        )
    }
}
