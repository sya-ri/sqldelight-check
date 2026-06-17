package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.FixSafety
import kotlin.test.Test
import kotlin.test.assertEquals

class NoBlankLinesInStatementRuleTest {
    @Test
    fun `reports blank line inside sq query statement`() {
        val content =
            """
            selectById:
            SELECT id, name
            FROM player

            WHERE id = :id;
            """.asSqlDelightFile()

        val diagnostics = NoBlankLinesInStatementRule().diagnostics(content)

        assertEquals(1, diagnostics.size)
        assertEquals(FixSafety.Safe, diagnostics.single().fixes.single().safety)
        assertEquals(
            """
            selectById:
            SELECT id, name
            FROM player
            WHERE id = :id;
            """.asSqlDelightFile(),
            NoBlankLinesInStatementRule().applySingleFix(content),
        )
    }

    @Test
    fun `reports blank line inside schema statement`() {
        NoBlankLinesInStatementRule().assertDiagnosticCount(
            """
            CREATE TABLE player (
              id INTEGER NOT NULL PRIMARY KEY,

              name TEXT NOT NULL
            );
            """.asSqlDelightFile(),
            1,
        )
    }

    @Test
    fun `reports blank line inside migration statement`() {
        NoBlankLinesInStatementRule().assertDiagnosticCount(
            """
            UPDATE player
            SET score = 0

            WHERE score IS NULL;
            """.asSqlDelightFile(),
            1,
            path = MIGRATION_SQM_PATH,
        )
    }

    @Test
    fun `reports separate blank line runs independently`() {
        NoBlankLinesInStatementRule().assertDiagnosticCount(
            """
            selectById:
            SELECT id,

              name
            FROM player

            WHERE id = :id;
            """.asSqlDelightFile(),
            2,
        )
    }

    @Test
    fun `accepts blank line between statements`() {
        NoBlankLinesInStatementRule().assertDiagnosticCount(
            """
            selectAll:
            SELECT * FROM player;

            selectById:
            SELECT * FROM player WHERE id = :id;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `does not duplicate blank line after query label`() {
        NoBlankLinesInStatementRule().assertDiagnosticCount(
            """
            selectAll:

            SELECT * FROM player;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `accepts clean sq and sqm fixtures`() {
        NoBlankLinesInStatementRule().assertDiagnosticCount(cleanPlayerSq, 0)
        NoBlankLinesInStatementRule().assertDiagnosticCount(cleanMigrationSqm, 0, path = MIGRATION_SQM_PATH)
    }

    @Test
    fun `ignores semicolons in strings and nested expressions`() {
        NoBlankLinesInStatementRule().assertDiagnosticCount(
            """
            selectLiteral:
            SELECT ';' AS value,
              (SELECT 1)
            FROM player;
            """.asSqlDelightFile(),
            0,
        )
    }
}
