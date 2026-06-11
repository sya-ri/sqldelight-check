package dev.s7a.sqldelight.check.rules.standard.rules

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MaxJoinsRuleTest {
    @Test
    fun `reports select with more joins than configured max`() {
        val diagnostics =
            MaxJoinsRule().diagnostics(
                """
                selectGraph:
                SELECT player.id
                FROM player
                JOIN team ON team.id = player.team_id
                JOIN league ON league.id = team.league_id;
                """.asSqlDelightFile(),
                options = mapOf("max" to "1"),
            )

        assertEquals(1, diagnostics.size)
        assertEquals(5, diagnostics.single().range?.start?.line)
    }

    @Test
    fun `accepts select within configured max joins`() {
        MaxJoinsRule().assertDiagnosticCount(
            """
            selectGraph:
            SELECT player.id
            FROM player
            JOIN team ON team.id = player.team_id;
            """.asSqlDelightFile(),
            0,
            options = mapOf("max" to "1"),
        )
    }

    @Test
    fun `rejects invalid max option`() {
        assertFailsWith<IllegalArgumentException> {
            MaxJoinsRule().diagnostics(cleanPlayerSq, options = mapOf("max" to "nope"))
        }
    }
}
