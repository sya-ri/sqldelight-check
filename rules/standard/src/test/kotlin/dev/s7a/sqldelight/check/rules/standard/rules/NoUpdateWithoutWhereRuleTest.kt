package dev.s7a.sqldelight.check.rules.standard.rules

import kotlin.test.Test
import kotlin.test.assertEquals

class NoUpdateWithoutWhereRuleTest {
    @Test
    fun `reports update without where clause`() {
        val diagnostics =
            NoUpdateWithoutWhereRule().diagnostics(
                """
                resetScores:
                UPDATE player
                SET score = 0;
                """.asSqlDelightFile(),
            )

        assertEquals(1, diagnostics.size)
        assertEquals(2, diagnostics.single().range?.start?.line)
    }

    @Test
    fun `accepts update with where clause`() {
        NoUpdateWithoutWhereRule().assertDiagnosticCount(
            """
            updatePlayer:
            UPDATE player
            SET score = ?
            WHERE id = ?;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `does not use nested where clause for update statement`() {
        NoUpdateWithoutWhereRule().assertDiagnosticCount(
            """
            updatePlayers:
            UPDATE player
            SET score = (
              SELECT max(score)
              FROM score
              WHERE score.player_id = player.id
            );
            """.asSqlDelightFile(),
            1,
        )
    }
}
