package dev.s7a.sqldelight.check.rules.standard.rules

import kotlin.test.Test
import kotlin.test.assertEquals

class NoLeadingCommaRuleTest {
    @Test
    fun `reports line-leading commas in sq queries`() {
        val diagnostics =
            NoLeadingCommaRule().diagnostics(
                """
                selectNames:
                SELECT id
                  , name
                  , score
                FROM player;
                """.asSqlDelightFile(),
            )

        assertEquals(2, diagnostics.size)
        assertEquals(0, diagnostics.first().fixes.size)
    }

    @Test
    fun `reports line-leading commas in migration files`() {
        NoLeadingCommaRule().assertDiagnosticCount(
            """
            CREATE TABLE player (
              id INTEGER NOT NULL
              , name TEXT NOT NULL
            );
            """.asSqlDelightFile(),
            1,
            path = MIGRATION_SQM_PATH,
        )
    }

    @Test
    fun `accepts trailing commas and single line sql`() {
        NoLeadingCommaRule().assertDiagnosticCount(
            """
            selectNames:
            SELECT id,
              name,
              score
            FROM player;
            """.asSqlDelightFile(),
            0,
        )
        NoLeadingCommaRule().assertDiagnosticCount("SELECT id, name FROM player;", 0)
    }

    @Test
    fun `ignores comments strings and quoted identifiers`() {
        NoLeadingCommaRule().assertDiagnosticCount(
            """
            selectLiteral:
            -- , name
            SELECT ', name', ", name", `, name`, [, name]
            FROM player;
            """.asSqlDelightFile(),
            0,
        )
    }
}
