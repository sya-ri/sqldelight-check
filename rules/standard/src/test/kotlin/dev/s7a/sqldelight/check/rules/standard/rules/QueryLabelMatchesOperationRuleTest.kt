package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.Severity
import kotlin.test.Test
import kotlin.test.assertEquals

class QueryLabelMatchesOperationRuleTest {
    @Test
    fun `reports label that does not match statement operation`() {
        val diagnostics =
            QueryLabelMatchesOperationRule().diagnostics(
                """
                deletePlayer:
                SELECT id FROM player;
                """.asSqlDelightFile(),
            )

        assertEquals(1, diagnostics.size)
        assertEquals(Severity.Info, diagnostics.single().severity)
        assertEquals(0, diagnostics.single().fixes.size)
    }

    @Test
    fun `accepts common operation prefixes`() {
        QueryLabelMatchesOperationRule().assertDiagnosticCount(
            """
            findById:
            SELECT id FROM player;

            upsertPlayer {
              UPDATE player SET name = :name;
              INSERT INTO player(id, name) VALUES (:id, :name);
            }
            """.asSqlDelightFile(),
            expected = 0,
        )
    }
}
