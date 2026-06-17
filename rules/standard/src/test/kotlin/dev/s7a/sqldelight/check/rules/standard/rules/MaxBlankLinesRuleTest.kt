package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.FixSafety
import kotlin.test.Test
import kotlin.test.assertEquals

class MaxBlankLinesRuleTest {
    @Test
    fun `reports extra blank lines between sq schema and named query sections`() {
        val diagnostics =
            MaxBlankLinesRule().diagnostics(
                """
                CREATE TABLE player (
                  id INTEGER NOT NULL PRIMARY KEY
                );


                selectAll:
                SELECT id
                FROM player;
                """.asSqlDelightFile(),
            )

        assertEquals(1, diagnostics.size)
        assertEquals(FixSafety.Safe, diagnostics.single().fixes.single().safety)
        assertEquals("", diagnostics.single().fixes.single().edits.single().replacement)
        MaxBlankLinesRule().assertAllFixes(
            """
            CREATE TABLE player (
              id INTEGER NOT NULL PRIMARY KEY
            );


            selectAll:
            SELECT id
            FROM player;
            """.asSqlDelightFile(),
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
    fun `accepts one blank line between sq declarations`() {
        MaxBlankLinesRule().assertDiagnosticCount(cleanPlayerSq, 0)
    }

    @Test
    fun `treats whitespace only lines as blank in migration files`() {
        MaxBlankLinesRule().assertDiagnosticCount(
            """
            CREATE TABLE player (
              id INTEGER NOT NULL PRIMARY KEY
            );

            <SP><SP>
            ALTER TABLE player ADD COLUMN score INTEGER NOT NULL DEFAULT 0;
            """.asSqlDelightFile().withSpaces(),
            1,
            path = MIGRATION_SQM_PATH,
        )
    }

    @Test
    fun `reports separate blank line runs independently in sq files`() {
        val content =
            """
            CREATE TABLE player (
              id INTEGER NOT NULL PRIMARY KEY
            );


            selectAll:
            SELECT id
            FROM player;


            selectById:
            SELECT id
            FROM player
            WHERE id = ?;
            """.asSqlDelightFile()

        MaxBlankLinesRule().assertDiagnosticCount(content, 2)
    }
}
