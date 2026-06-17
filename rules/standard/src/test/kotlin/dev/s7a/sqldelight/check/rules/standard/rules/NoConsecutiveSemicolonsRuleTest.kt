package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.FixSafety
import kotlin.test.Test
import kotlin.test.assertEquals

class NoConsecutiveSemicolonsRuleTest {
    @Test
    fun `reports safe fix in sq named query`() {
        val diagnostics =
            NoConsecutiveSemicolonsRule().diagnostics(
                """
                selectAll:
                SELECT id
                FROM player
                ORDER BY name;;
                """.asSqlDelightFile(),
            )

        assertEquals(1, diagnostics.size)
        assertEquals(FixSafety.Safe, diagnostics.single().fixes.single().safety)
        assertEquals("", diagnostics.single().fixes.single().edits.single().replacement)
        NoConsecutiveSemicolonsRule().assertAllFixes(
            """
            selectAll:
            SELECT id
            FROM player
            ORDER BY name;;
            """.asSqlDelightFile(),
            """
            selectAll:
            SELECT id
            FROM player
            ORDER BY name;
            """.asSqlDelightFile(),
        )
    }

    @Test
    fun `reports one diagnostic for a semicolon run in migration`() {
        val diagnostics =
            NoConsecutiveSemicolonsRule()
                .diagnostics(
                    """
                    CREATE TABLE player (
                      id INTEGER NOT NULL PRIMARY KEY
                    );;;;;
                    """.asSqlDelightFile(),
                    path = MIGRATION_SQM_PATH,
                )

        assertEquals(1, diagnostics.size)
    }

    @Test
    fun `accepts clean sq and sqm statement terminators`() {
        NoConsecutiveSemicolonsRule().assertDiagnosticCount(cleanPlayerSq, 0)
        NoConsecutiveSemicolonsRule().assertDiagnosticCount(cleanMigrationSqm, 0, path = MIGRATION_SQM_PATH)
    }

    @Test
    fun `reports separate semicolon runs independently in sq file`() {
        val content =
            """
            selectAll:
            SELECT id
            FROM player
            ORDER BY name;;

            selectById:
            SELECT id
            FROM player
            WHERE id = ?;;
            """.asSqlDelightFile()

        NoConsecutiveSemicolonsRule().assertDiagnosticCount(content, 2)
    }

    @Test
    fun `does not cross whitespace between semicolons in sq file`() {
        NoConsecutiveSemicolonsRule().assertDiagnosticCount(
            """
            selectAll:
            SELECT id
            FROM player
            ORDER BY name; ;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `ignores comments strings and quoted identifiers in sq files`() {
        val content =
            """
            selectLiteral:
            -- SELECT 1;;
            SELECT ';;', ";;", `;;`, [;;];
            """.asSqlDelightFile()

        NoConsecutiveSemicolonsRule().assertDiagnosticCount(content, 0)
    }
}
