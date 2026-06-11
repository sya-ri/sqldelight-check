package dev.s7a.sqldelight.check.rules.standard.rules

import kotlin.test.Test
import kotlin.test.assertEquals

class NoTransactionInMigrationRuleTest {
    @Test
    fun `reports explicit transaction statements in migration files`() {
        val diagnostics =
            NoTransactionInMigrationRule().diagnostics(
                """
                BEGIN TRANSACTION;
                ALTER TABLE player ADD COLUMN score INTEGER;
                COMMIT;
                """.asSqlDelightFile(),
                path = MIGRATION_SQM_PATH,
            )

        assertEquals(2, diagnostics.size)
        assertEquals(0, diagnostics.first().fixes.size)
    }

    @Test
    fun `reports rollback and end transaction in migration files`() {
        val diagnostics =
            NoTransactionInMigrationRule().diagnostics(
                """
                BEGIN;
                ALTER TABLE player ADD COLUMN score INTEGER;
                ROLLBACK;
                END TRANSACTION;
                """.asSqlDelightFile(),
                path = MIGRATION_SQM_PATH,
            )

        assertEquals(3, diagnostics.size)
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
