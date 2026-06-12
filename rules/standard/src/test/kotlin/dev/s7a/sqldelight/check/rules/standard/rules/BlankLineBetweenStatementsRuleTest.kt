package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.DialectFamily
import dev.s7a.sqldelight.check.api.SqlDialect
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatterns
import dev.s7a.sqldelight.check.api.SqlDialectSourcePattern
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.StatementStart
import kotlin.test.Test
import kotlin.test.assertEquals

class BlankLineBetweenStatementsRuleTest {
    @Test
    fun `reports adjacent sqldelight labeled statements`() {
        val content =
            """
            selectAll:
            SELECT * FROM player;
            selectById:
            SELECT * FROM player WHERE id = :id;
            """.asSqlDelightFile()

        val diagnostics = BlankLineBetweenStatementsRule().diagnostics(content)

        assertEquals(1, diagnostics.size)
        assertEquals(
            """
            selectAll:
            SELECT * FROM player;

            selectById:
            SELECT * FROM player WHERE id = :id;
            """.asSqlDelightFile(),
            BlankLineBetweenStatementsRule().applySingleFix(content),
        )
    }

    @Test
    fun `reports adjacent schema and query statements`() {
        BlankLineBetweenStatementsRule().assertDiagnosticCount(
            """
            CREATE TABLE player (
              id INTEGER NOT NULL PRIMARY KEY
            );
            selectAll:
            SELECT * FROM player;
            """.asSqlDelightFile(),
            1,
        )
    }

    @Test
    fun `reports adjacent migration statements`() {
        BlankLineBetweenStatementsRule().assertDiagnosticCount(
            """
            ALTER TABLE player ADD COLUMN age INTEGER;
            UPDATE player SET age = 0;
            """.asSqlDelightFile(),
            1,
            path = MIGRATION_SQM_PATH,
        )
    }

    @Test
    fun `accepts one blank line between statements`() {
        BlankLineBetweenStatementsRule().assertDiagnosticCount(
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
    fun `accepts clean sq and sqm fixtures`() {
        BlankLineBetweenStatementsRule().assertDiagnosticCount(cleanPlayerSq, 0)
        BlankLineBetweenStatementsRule().assertDiagnosticCount(cleanMigrationSqm, 0, path = MIGRATION_SQM_PATH)
    }

    @Test
    fun `ignores semicolons in nested expressions and strings`() {
        BlankLineBetweenStatementsRule().assertDiagnosticCount(
            """
            selectLiteral:
            SELECT ';' AS value, (SELECT 1);
            selectNext:
            SELECT 2;
            """.asSqlDelightFile(),
            1,
        )
    }

    @Test
    fun `uses dialect source patterns for custom statement starts`() {
        val dialect =
            SqlDialect(
                family = DialectFamily.Unknown,
                sourcePatterns =
                    SqlDialectSourcePatterns(
                        patterns =
                            SqlDialectSourcePatterns.SourceScannerDefault.patterns +
                                SqlDialectSourcePattern.parse("ANALYZE", StatementStart),
                    ),
            )

        BlankLineBetweenStatementsRule().assertDiagnosticCount(
            """
            SELECT 1;
            ANALYZE player;
            """.asSqlDelightFile(),
            1,
            path = MIGRATION_SQM_PATH,
            dialect = dialect,
        )
    }
}
