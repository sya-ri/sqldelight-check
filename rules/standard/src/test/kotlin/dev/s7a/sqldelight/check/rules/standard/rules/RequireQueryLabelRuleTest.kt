package dev.s7a.sqldelight.check.rules.standard.rules

import kotlin.test.Test
import kotlin.test.assertEquals

class RequireQueryLabelRuleTest {
    @Test
    fun `reports executable statements without query labels`() {
        val diagnostics =
            RequireQueryLabelRule().diagnostics(
                """
                SELECT id, name
                FROM player;

                UPDATE player
                SET name = :name;
                """.asSqlDelightFile(),
            )

        assertEquals(2, diagnostics.size)
        assertEquals(0, diagnostics.first().fixes.size)
    }

    @Test
    fun `accepts labeled and grouped statements`() {
        RequireQueryLabelRule().assertDiagnosticCount(
            """
            selectAll:
            SELECT id, name
            FROM player;

            upsertPlayer {
              UPDATE player
              SET name = :name
              WHERE id = :id;
            }
            """.asSqlDelightFile(),
            expected = 0,
        )
    }

    @Test
    fun `ignores schema statements and migrations`() {
        RequireQueryLabelRule().assertDiagnosticCount(
            """
            CREATE TABLE player (
              id INTEGER NOT NULL PRIMARY KEY
            );

            CREATE VIEW player_view AS
            SELECT id
            FROM player;
            """.asSqlDelightFile(),
            expected = 0,
        )
        RequireQueryLabelRule().assertDiagnosticCount(
            """
            UPDATE player SET name = 'name';
            """.asSqlDelightFile(),
            expected = 0,
            path = MIGRATION_SQM_PATH,
        )
    }
}
