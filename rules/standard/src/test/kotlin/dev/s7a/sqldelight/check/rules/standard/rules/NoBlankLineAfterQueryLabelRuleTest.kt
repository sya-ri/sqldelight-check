package dev.s7a.sqldelight.check.rules.standard.rules

import kotlin.test.Test
import kotlin.test.assertEquals

class NoBlankLineAfterQueryLabelRuleTest {
    @Test
    fun `reports blank line after query label`() {
        val content =
            """
            selectPlayers:

            SELECT id
            FROM player;
            """.asSqlDelightFile()

        val diagnostics = NoBlankLineAfterQueryLabelRule().diagnostics(content)

        assertEquals(1, diagnostics.size)
        assertEquals(
            """
            selectPlayers:
            SELECT id
            FROM player;
            """.asSqlDelightFile(),
            NoBlankLineAfterQueryLabelRule().applySingleFix(content),
        )
    }

    @Test
    fun `reports multiple blank lines after grouped statement label`() {
        NoBlankLineAfterQueryLabelRule().assertDiagnosticCount(
            """
            upsertPlayer {


              UPDATE player
              SET name = :name;
            }
            """.asSqlDelightFile(),
            1,
        )
    }

    @Test
    fun `accepts label attached to statement body`() {
        NoBlankLineAfterQueryLabelRule().assertDiagnosticCount(
            """
            selectPlayers:
            SELECT id
            FROM player;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `ignores migration files`() {
        NoBlankLineAfterQueryLabelRule().assertDiagnosticCount(
            """
            notALabel:

            SELECT 1;
            """.asSqlDelightFile(),
            0,
            path = MIGRATION_SQM_PATH,
        )
    }
}
