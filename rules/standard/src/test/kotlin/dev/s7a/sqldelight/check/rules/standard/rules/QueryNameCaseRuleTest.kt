package dev.s7a.sqldelight.check.rules.standard.rules

import kotlin.test.Test
import kotlin.test.assertEquals

class QueryNameCaseRuleTest {
    @Test
    fun `reports non lower camel query labels`() {
        val diagnostics =
            QueryNameCaseRule().diagnostics(
                """
                Select_All:
                SELECT id, name
                FROM player;

                _insertPlayer:
                INSERT INTO player(id, name)
                VALUES (:id, :name);
                """.asSqlDelightFile(),
            )

        assertEquals(2, diagnostics.size)
        assertEquals(0, diagnostics.first().fixes.size)
    }

    @Test
    fun `accepts lower camel query labels and grouped statements`() {
        QueryNameCaseRule().assertDiagnosticCount(
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
    fun `ignores schema comments casts and migrations`() {
        QueryNameCaseRule().assertDiagnosticCount(
            """
            -- Bad_Label:
            CREATE TABLE player (
              id INTEGER NOT NULL PRIMARY KEY
            );

            selectCast:
            SELECT value::TEXT
            FROM player;
            """.asSqlDelightFile(),
            expected = 0,
        )
        QueryNameCaseRule().assertDiagnosticCount(
            """
            Bad_Migration_Label:
            SELECT 1;
            """.asSqlDelightFile(),
            expected = 0,
            path = MIGRATION_SQM_PATH,
        )
    }
}
