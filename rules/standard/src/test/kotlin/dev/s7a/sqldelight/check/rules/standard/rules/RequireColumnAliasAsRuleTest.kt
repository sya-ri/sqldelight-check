package dev.s7a.sqldelight.check.rules.standard.rules

import kotlin.test.Test
import kotlin.test.assertEquals

class RequireColumnAliasAsRuleTest {
    @Test
    fun `reports implicit result aliases`() {
        val diagnostics =
            RequireColumnAliasAsRule().diagnostics(
                """
                selectStats:
                SELECT count(*) total, score + 1 next_score
                FROM player;
                """.asSqlDelightFile(),
            )

        assertEquals(2, diagnostics.size)
        assertEquals("Column aliases should use AS.", diagnostics.first().message)
    }

    @Test
    fun `accepts explicit result aliases`() {
        RequireColumnAliasAsRule().assertDiagnosticCount(
            """
            selectStats:
            SELECT count(*) AS total, score + 1 AS next_score
            FROM player;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `ignores unaliased simple columns and computed expressions`() {
        RequireColumnAliasAsRule().assertDiagnosticCount(
            """
            selectStats:
            SELECT id, score + 1
            FROM player;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `tracks nested select aliases independently`() {
        RequireColumnAliasAsRule().assertDiagnosticCount(
            """
            selectStats:
            SELECT total
            FROM (
              SELECT count(*) total
              FROM player
            ) AS ranked;
            """.asSqlDelightFile(),
            1,
        )
    }
}
