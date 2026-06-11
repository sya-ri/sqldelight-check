package dev.s7a.sqldelight.check.rules.standard.rules

import kotlin.test.Test
import kotlin.test.assertEquals

class PreferExplicitColumnListInInsertRuleTest {
    @Test
    fun `reports insert without column list`() {
        val diagnostics =
            PreferExplicitColumnListInInsertRule().diagnostics(
                """
                insertPlayer:
                INSERT INTO player
                VALUES (?, ?, ?);
                """.asSqlDelightFile(),
            )

        assertEquals(1, diagnostics.size)
        assertEquals(2, diagnostics.single().range?.start?.line)
    }

    @Test
    fun `accepts insert with column list`() {
        PreferExplicitColumnListInInsertRule().assertDiagnosticCount(
            """
            insertPlayer:
            INSERT INTO player(id, name, score)
            VALUES (?, ?, ?);
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `accepts insert select with column list`() {
        PreferExplicitColumnListInInsertRule().assertDiagnosticCount(
            """
            copyPlayers:
            INSERT INTO archived_player(id, name, score)
            SELECT id, name, score
            FROM player;
            """.asSqlDelightFile(),
            0,
        )
    }
}
