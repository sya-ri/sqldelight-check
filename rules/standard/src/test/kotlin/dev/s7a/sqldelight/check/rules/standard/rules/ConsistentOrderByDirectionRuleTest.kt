package dev.s7a.sqldelight.check.rules.standard.rules

import kotlin.test.Test
import kotlin.test.assertEquals

class ConsistentOrderByDirectionRuleTest {
    @Test
    fun `reports mixed explicit and implicit order by directions`() {
        val diagnostics =
            ConsistentOrderByDirectionRule().diagnostics(
                """
                selectPlayers:
                SELECT id, name, score
                FROM player
                ORDER BY name, score DESC;
                """.asSqlDelightFile(),
            )

        assertEquals(1, diagnostics.size)
        assertEquals(0, diagnostics.single().fixes.size)
    }

    @Test
    fun `reports mixed order by directions in migration files`() {
        val diagnostics =
            ConsistentOrderByDirectionRule().diagnostics(
                """
                INSERT INTO player_rank_snapshot(player_id, score)
                SELECT id, score
                FROM player
                ORDER BY score DESC, id;
                """.asSqlDelightFile(),
                path = MIGRATION_SQM_PATH,
            )

        assertEquals(1, diagnostics.size)
    }

    @Test
    fun `accepts all explicit and all implicit directions`() {
        ConsistentOrderByDirectionRule().assertDiagnosticCount(
            """
            selectPlayersByName:
            SELECT id, name, score
            FROM player
            ORDER BY name, score;

            selectPlayersByScore:
            SELECT id, name, score
            FROM player
            ORDER BY score DESC, name ASC;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `stops order by at limit`() {
        ConsistentOrderByDirectionRule().assertDiagnosticCount(
            """
            selectPlayers:
            SELECT id, name, score
            FROM player
            ORDER BY score DESC
            LIMIT 10;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `ignores comments strings and quoted identifiers`() {
        ConsistentOrderByDirectionRule().assertDiagnosticCount(
            """
            selectLiteral:
            -- ORDER BY name, score DESC
            SELECT 'ORDER BY name, score DESC', "ORDER", `BY`, [ORDER BY]
            FROM player;
            """.asSqlDelightFile(),
            0,
        )
    }
}
