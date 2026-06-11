package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.DialectFamily
import dev.s7a.sqldelight.check.api.SqlDialect
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatterns
import dev.s7a.sqldelight.check.api.SqlDialectSourcePattern
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.SqlDelightStatementStart
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.StatementContinuation
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.StatementStart
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StatementTerminatorRuleTest {
    @Test
    fun `reports missing terminator at end of sqm statement`() {
        val diagnostics =
            StatementTerminatorRule().diagnostics(
                """
                CREATE TABLE player (
                  id INTEGER NOT NULL PRIMARY KEY,
                  name TEXT NOT NULL
                )
                """.asSqlDelightFile(),
                path = MIGRATION_SQM_PATH,
            )

        assertEquals(1, diagnostics.size)
        assertTrue(diagnostics.single().fixes.isEmpty())
    }

    @Test
    fun `reports missing terminator before next sqm statement`() {
        val content =
            """
            CREATE TABLE player (
              id INTEGER NOT NULL PRIMARY KEY
            )

            ALTER TABLE player ADD COLUMN score INTEGER NOT NULL DEFAULT 0;
            """.asSqlDelightFile()

        StatementTerminatorRule().assertDiagnosticCount(content, 1, path = MIGRATION_SQM_PATH)
    }

    @Test
    fun `reports every unterminated sqm statement block`() {
        val content =
            """
            CREATE TABLE player (
              id INTEGER NOT NULL PRIMARY KEY
            )

            INSERT INTO player(id)
            VALUES (1)

            UPDATE player
            SET id = 2
            """.asSqlDelightFile()

        StatementTerminatorRule().assertDiagnosticCount(content, 3, path = MIGRATION_SQM_PATH)
    }

    @Test
    fun `accepts terminated sqm ddl and dml statements`() {
        val content =
            """
            CREATE TABLE player (
              id INTEGER NOT NULL PRIMARY KEY
            );

            INSERT INTO player(id)
            VALUES (1);

            UPDATE player
            SET id = 2;

            DELETE FROM player
            WHERE id = 2;
            """.asSqlDelightFile()

        StatementTerminatorRule().assertDiagnosticCount(content, 0, path = MIGRATION_SQM_PATH)
    }

    @Test
    fun `reports missing terminator at end of sq named query`() {
        val diagnostics =
            StatementTerminatorRule().diagnostics(
                """
                selectAll:
                SELECT id, name
                FROM player
                ORDER BY name
                """.asSqlDelightFile(),
            )

        assertEquals(1, diagnostics.size)
        assertTrue(diagnostics.single().fixes.isEmpty())
    }

    @Test
    fun `reports missing terminator before next sq label`() {
        val content =
            """
            selectAll:
            SELECT id, name
            FROM player
            ORDER BY name

            selectById:
            SELECT id, name
            FROM player
            WHERE id = ?;
            """.asSqlDelightFile()

        StatementTerminatorRule().assertDiagnosticCount(content, 1)
    }

    @Test
    fun `accepts terminated sq schema statements and named queries`() {
        StatementTerminatorRule().assertDiagnosticCount(cleanPlayerSq, 0)
    }

    @Test
    fun `reports unterminated sq schema statement`() {
        val content =
            """
            CREATE TABLE player (
              id INTEGER NOT NULL PRIMARY KEY
            )

            selectAll:
            SELECT id
            FROM player;
            """.asSqlDelightFile()

        StatementTerminatorRule().assertDiagnosticCount(content, 1)
    }

    @Test
    fun `ignores sql words without statement labels in sq files`() {
        val content =
            """
            import com.example.PlayerId;

            selectAll:
            SELECT id AS PlayerId
            FROM player;
            """.asSqlDelightFile()

        StatementTerminatorRule().assertDiagnosticCount(content, 0)
    }

    @Test
    fun `ignores comments strings and quoted identifiers in sq files`() {
        val content =
            """
            selectLiteral:
            -- SELECT 1
            SELECT ';', "unterminated", `unterminated`, [unterminated];
            """.asSqlDelightFile()

        StatementTerminatorRule().assertDiagnosticCount(content, 0)
    }

    @Test
    fun `accepts with query terminated in sq file`() {
        val content =
            """
            selectTopPlayers:
            WITH ranked AS (
              SELECT id, score
              FROM player
            )
            SELECT id
            FROM ranked;
            """.asSqlDelightFile()

        StatementTerminatorRule().assertDiagnosticCount(content, 0)
    }

    @Test
    fun `accepts insert select query terminated in sq file`() {
        val content =
            """
            copyPlayers:
            INSERT INTO player_backup(id, name)
            SELECT id, name
            FROM player;
            """.asSqlDelightFile()

        StatementTerminatorRule().assertDiagnosticCount(content, 0)
    }

    @Test
    fun `accepts create view as select terminated in sqm file`() {
        val content =
            """
            CREATE VIEW active_player AS
            SELECT id, name
            FROM player
            WHERE active = 1;
            """.asSqlDelightFile()

        StatementTerminatorRule().assertDiagnosticCount(content, 0, path = MIGRATION_SQM_PATH)
    }

    @Test
    fun `reports with query missing terminator in sq file`() {
        val content =
            """
            selectTopPlayers:
            WITH ranked AS (
              SELECT id, score
              FROM player
            )
            SELECT id
            FROM ranked
            """.asSqlDelightFile()

        StatementTerminatorRule().assertDiagnosticCount(content, 1)
    }

    @Test
    fun `uses dialect source patterns for custom statement continuations`() {
        val dialect =
            SqlDialect(
                family = DialectFamily.Custom,
                sourcePatterns =
                    SqlDialectSourcePatterns(
                        patterns =
                            SqlDialectSourcePatterns.SourceScannerDefault.patterns +
                                SqlDialectSourcePattern.parse("EXPLAIN", StatementStart, SqlDelightStatementStart) +
                                SqlDialectSourcePattern.parse("EXPLAIN SELECT", StatementContinuation),
                    ),
            )
        val content =
            """
            EXPLAIN
            SELECT id
            FROM player;
            """.asSqlDelightFile()

        StatementTerminatorRule().assertDiagnosticCount(content, 0, path = MIGRATION_SQM_PATH, dialect = dialect)
    }
}
