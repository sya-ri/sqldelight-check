package dev.s7a.sqldelight.check.rules.standard.rules

import kotlin.test.Test
import kotlin.test.assertEquals

class UniqueColumnAliasesRuleTest {
    @Test
    fun `reports duplicate result aliases`() {
        val diagnostics =
            UniqueColumnAliasesRule().diagnostics(
                """
                selectStats:
                SELECT count(*) AS total, max(score) AS total
                FROM player;
                """.asSqlDelightFile(),
            )

        assertEquals(1, diagnostics.size)
        assertEquals("Column aliases should be unique within a SELECT list.", diagnostics.single().message)
    }

    @Test
    fun `reports duplicate implicit result aliases`() {
        UniqueColumnAliasesRule().assertDiagnosticCount(
            """
            selectStats:
            SELECT count(*) total, max(score) total
            FROM player;
            """.asSqlDelightFile(),
            1,
        )
    }

    @Test
    fun `accepts duplicate aliases in separate select lists`() {
        UniqueColumnAliasesRule().assertDiagnosticCount(
            """
            selectStats:
            SELECT total
            FROM (
              SELECT count(*) AS total
              FROM player
            ) AS ranked;

            selectScores:
            SELECT max(score) AS total
            FROM player;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `accepts unique result aliases`() {
        UniqueColumnAliasesRule().assertDiagnosticCount(
            """
            selectStats:
            SELECT count(*) AS total, max(score) AS max_score
            FROM player;
            """.asSqlDelightFile(),
            0,
        )
    }
}
