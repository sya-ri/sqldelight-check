package dev.s7a.sqldelight.check.rules.standard.rules

import kotlin.test.Test
import kotlin.test.assertEquals

class ClauseKeywordNewlineRuleTest {
    @Test
    fun `reports top-level clause keywords after other code on multiline select statements`() {
        val diagnostics =
            ClauseKeywordNewlineRule().diagnostics(
                """
                selectNames:
                SELECT name FROM player WHERE score > 0
                AND active = 1 GROUP BY name HAVING count(*) > 1
                AND archived = 0 ORDER BY name LIMIT 10 OFFSET 5;
                """.asSqlDelightFile(),
            )

        assertEquals(7, diagnostics.size)
        assertEquals(0, diagnostics.first().fixes.size)
    }

    @Test
    fun `reports clause keywords in migration files`() {
        ClauseKeywordNewlineRule().assertDiagnosticCount(
            """
            INSERT INTO active_name(name)
            SELECT name FROM player
            WHERE active = 1;
            """.asSqlDelightFile(),
            1,
            path = MIGRATION_SQM_PATH,
        )
    }

    @Test
    fun `accepts top-level clause keywords at beginning of line after indentation`() {
        ClauseKeywordNewlineRule().assertDiagnosticCount(
            """
            selectNames:
            SELECT name
              FROM player
            WHERE score > 0
            GROUP BY name
              HAVING count(*) > 1
            ORDER BY name
              LIMIT 10
            OFFSET 5;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `accepts SQLDelight named parameters that match clause keywords`() {
        ClauseKeywordNewlineRule().assertDiagnosticCount(
            """
            selectPlayers:
            SELECT id, name
            FROM player
            ORDER BY name, id
            LIMIT :limit
            OFFSET :offset;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `accepts single line and skips nested parenthesized select statements`() {
        ClauseKeywordNewlineRule().assertDiagnosticCount("SELECT name FROM player WHERE id = ?;", 0)
        ClauseKeywordNewlineRule().assertDiagnosticCount(
            """
            selectNames:
            SELECT name
            FROM (
              SELECT name FROM player
              WHERE active = 1
            );
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `ignores comments strings and quoted identifiers`() {
        ClauseKeywordNewlineRule().assertDiagnosticCount(
            """
            selectLiteral:
            -- SELECT name FROM player WHERE id = 1
            SELECT 'FROM', "WHERE", `GROUP BY`, [ORDER BY]
            FROM player;
            """.asSqlDelightFile(),
            0,
        )
    }
}
