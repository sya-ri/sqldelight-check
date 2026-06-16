package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.FixSafety
import kotlin.test.Test
import kotlin.test.assertEquals

class NoTransactionInMigrationRuleTest {
    @Test
    fun `reports explicit transaction statements in migration files`() {
        val content =
            """
            BEGIN TRANSACTION;
            ALTER TABLE player ADD COLUMN score INTEGER;
            COMMIT;
            """.asSqlDelightFile()
        val diagnostics =
            NoTransactionInMigrationRule().diagnostics(
                content,
                path = MIGRATION_SQM_PATH,
            )

        assertEquals(2, diagnostics.size)
        assertEquals(FixSafety.Unsafe, diagnostics.first().fixes.single().safety)
        NoTransactionInMigrationRule().assertAllFixes(
            content,
            """
            ALTER TABLE player ADD COLUMN score INTEGER;
            """.asSqlDelightFile(),
            path = MIGRATION_SQM_PATH,
        )
    }

    @Test
    fun `reports rollback and end transaction in migration files`() {
        val content =
            """
            BEGIN;
            ALTER TABLE player ADD COLUMN score INTEGER;
            ROLLBACK;
            END TRANSACTION;
            """.asSqlDelightFile()
        val diagnostics =
            NoTransactionInMigrationRule().diagnostics(
                content,
                path = MIGRATION_SQM_PATH,
            )

        assertEquals(3, diagnostics.size)
        assertEquals(FixSafety.Unsafe, diagnostics.first().fixes.single().safety)
        NoTransactionInMigrationRule().assertAllFixes(
            content,
            """
            ALTER TABLE player ADD COLUMN score INTEGER;
            """.asSqlDelightFile(),
            path = MIGRATION_SQM_PATH,
        )
    }

    @Test
    fun `accepts migration files without transaction statements`() {
        NoTransactionInMigrationRule().assertDiagnosticCount(
            cleanMigrationSqm,
            expected = 0,
            path = MIGRATION_SQM_PATH,
        )
    }

    @Test
    fun `accepts begin and end inside trigger bodies`() {
        NoTransactionInMigrationRule().assertDiagnosticCount(
            """
            CREATE TRIGGER player_updated
            AFTER UPDATE ON player
            BEGIN
              INSERT INTO player_log(player_id) VALUES (new.id);
            END;
            """.asSqlDelightFile(),
            expected = 0,
            path = MIGRATION_SQM_PATH,
        )
    }

    @Test
    fun `ignores sq files`() {
        NoTransactionInMigrationRule().assertDiagnosticCount(
            """
            createPlayer:
            BEGIN TRANSACTION;
            """.asSqlDelightFile(),
            expected = 0,
        )
    }

    @Test
    fun `ignores comments strings and quoted identifiers`() {
        NoTransactionInMigrationRule().assertDiagnosticCount(
            """
            -- BEGIN TRANSACTION;
            SELECT 'COMMIT', "ROLLBACK", `BEGIN`, [END];
            """.asSqlDelightFile(),
            expected = 0,
            path = MIGRATION_SQM_PATH,
        )
    }
}
