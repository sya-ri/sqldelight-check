package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.Severity
import kotlin.test.Test
import kotlin.test.assertEquals

class NoDeleteWithoutWhereRuleTest {
    @Test
    fun `reports delete without where clause`() {
        val diagnostics =
            NoDeleteWithoutWhereRule().diagnostics(
                """
                deletePlayers:
                DELETE FROM player;
                """.asSqlDelightFile(),
            )

        assertEquals(1, diagnostics.size)
        assertEquals(Severity.Error, diagnostics.single().severity)
        assertEquals(2, diagnostics.single().range?.start?.line)
    }

    @Test
    fun `accepts delete with where clause`() {
        NoDeleteWithoutWhereRule().assertDiagnosticCount(
            """
            deletePlayer:
            DELETE FROM player
            WHERE id = ?;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `accepts on delete cascade foreign key action`() {
        NoDeleteWithoutWhereRule().assertDiagnosticCount(
            """
            CREATE TABLE child (
              id INTEGER NOT NULL PRIMARY KEY,
              parent_id INTEGER NOT NULL REFERENCES parent(id) ON DELETE CASCADE
            );
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `does not use nested where clause for delete statement`() {
        NoDeleteWithoutWhereRule().assertDiagnosticCount(
            """
            deletePlayers:
            DELETE FROM player
            RETURNING (
              SELECT count(*)
              FROM score
              WHERE score.player_id = player.id
            );
            """.asSqlDelightFile(),
            1,
        )
    }
}
