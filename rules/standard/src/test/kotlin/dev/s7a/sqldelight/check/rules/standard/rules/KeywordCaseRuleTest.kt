package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.FixSafety
import kotlin.test.Test
import kotlin.test.assertEquals

class KeywordCaseRuleTest {
    @Test
    fun `reports lowercase keywords in sq named queries`() {
        val content =
            """
            CREATE TABLE player (
              id INTEGER NOT NULL PRIMARY KEY,
              name TEXT NOT NULL
            );

            selectById:
            select id, name
            from player
            where id = ?;
            """.asSqlDelightFile()
        val diagnostics = KeywordCaseRule().diagnostics(content)

        assertEquals(3, diagnostics.size)
        assertEquals(FixSafety.Unsafe, diagnostics.first().fixes.single().safety)
        assertEquals("SELECT", diagnostics.first().fixes.single().edits.single().replacement)
    }

    @Test
    fun `reports lowercase keywords in sqm migration statements`() {
        val diagnostics =
            KeywordCaseRule().diagnostics(
                """
                create table player (
                  id integer not null primary key
                );
                """.asSqlDelightFile(),
                path = MIGRATION_SQM_PATH,
            )

        assertEquals(6, diagnostics.size)
    }

    @Test
    fun `ignores comments and strings in sq files`() {
        val diagnostics =
            KeywordCaseRule().diagnostics(
                """
                -- select from where
                selectLiteral:
                SELECT 'select from where';
                """.asSqlDelightFile(),
            )

        assertEquals(0, diagnostics.size)
    }

    @Test
    fun `ignores quoted identifiers`() {
        val content =
            """
            SELECT "select", `from`, [where]
            FROM player;
            """.asSqlDelightFile()

        assertEquals(0, KeywordCaseRule().diagnostics(content).size)
    }

    @Test
    fun `accepts uppercase keywords`() {
        KeywordCaseRule().assertDiagnosticCount(cleanPlayerSq, 0)
    }

    @Test
    fun `does not report non keyword identifiers`() {
        KeywordCaseRule().assertDiagnosticCount("selectedFrom:\nSELECT selected FROM player;\n", 0)
    }
}
