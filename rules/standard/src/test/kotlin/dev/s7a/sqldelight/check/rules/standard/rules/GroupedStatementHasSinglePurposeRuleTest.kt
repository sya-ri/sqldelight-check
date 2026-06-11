package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.Severity
import kotlin.test.Test
import kotlin.test.assertEquals

class GroupedStatementHasSinglePurposeRuleTest {
    @Test
    fun `reports grouped statements that mix read and write statements`() {
        val diagnostics =
            GroupedStatementHasSinglePurposeRule().diagnostics(
                """
                updateAndRead {
                  UPDATE player
                  SET name = :name
                  WHERE id = :id;

                  SELECT id, name
                  FROM player;
                }
                """.asSqlDelightFile(),
            )

        assertEquals(1, diagnostics.size)
        assertEquals(Severity.Info, diagnostics.single().severity)
    }

    @Test
    fun `accepts write-only upsert groups`() {
        GroupedStatementHasSinglePurposeRule().assertDiagnosticCount(
            """
            upsertPlayer {
              UPDATE player
              SET name = :name
              WHERE id = :id;

              INSERT INTO player(id, name)
              VALUES (:id, :name);
            }
            """.asSqlDelightFile(),
            expected = 0,
        )
    }
}
