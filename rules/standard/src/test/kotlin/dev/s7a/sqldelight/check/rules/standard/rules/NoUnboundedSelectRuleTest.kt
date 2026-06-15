package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.Severity
import kotlin.test.Test
import kotlin.test.assertEquals

class NoUnboundedSelectRuleTest {
    @Test
    fun `reports query select without where or limit`() {
        val diagnostics =
            NoUnboundedSelectRule().diagnostics(
                """
                selectPlayers:
                SELECT id, name
                FROM player
                ORDER BY name;
                """.asSqlDelightFile(),
            )

        assertEquals(1, diagnostics.size)
        assertEquals(Severity.Info, diagnostics.single().severity)
    }

    @Test
    fun `ignores select with where`() {
        NoUnboundedSelectRule().assertDiagnosticCount(
            """
            selectById:
            SELECT id, name
            FROM player
            WHERE id = :id;
            """.asSqlDelightFile(),
            expected = 0,
        )
    }

    @Test
    fun `ignores select with limit`() {
        NoUnboundedSelectRule().assertDiagnosticCount(
            """
            selectFirst:
            SELECT id, name
            FROM player
            LIMIT 1;
            """.asSqlDelightFile(),
            expected = 0,
        )
    }

    @Test
    fun `ignores count query`() {
        NoUnboundedSelectRule().assertDiagnosticCount(
            """
            countPlayers:
            SELECT COUNT(*)
            FROM player;
            """.asSqlDelightFile(),
            expected = 0,
        )
    }

    @Test
    fun `ignores aggregate-only query`() {
        NoUnboundedSelectRule().assertDiagnosticCount(
            """
            countScores:
            SELECT COUNT(score)
            FROM player;

            minScore:
            SELECT MIN(score)
            FROM player;

            maxScore:
            SELECT MAX(score)
            FROM player;
            """.asSqlDelightFile(),
            expected = 0,
        )
    }

    @Test
    fun `ignores exists query`() {
        NoUnboundedSelectRule().assertDiagnosticCount(
            """
            hasPlayers:
            SELECT EXISTS(
              SELECT *
              FROM player
            );
            """.asSqlDelightFile(),
            expected = 0,
        )
    }

    @Test
    fun `ignores migration selects`() {
        NoUnboundedSelectRule().assertDiagnosticCount(
            """
            SELECT id, name
            FROM player;
            """.asSqlDelightFile(),
            expected = 0,
            path = MIGRATION_SQM_PATH,
        )
    }
}
