package dev.s7a.sqldelight.check.rules.standard.rules

import kotlin.test.Test
import kotlin.test.assertEquals

class RequireOrderByWithLimitRuleTest {
    @Test
    fun `reports select limit without order by`() {
        val diagnostics =
            RequireOrderByWithLimitRule().diagnostics(
                """
                selectPlayers:
                SELECT id, name
                FROM player
                LIMIT 10;
                """.asSqlDelightFile(),
            )

        assertEquals(1, diagnostics.size)
        assertEquals(0, diagnostics.single().fixes.size)
    }

    @Test
    fun `reports select offset without order by in migration files`() {
        val diagnostics =
            RequireOrderByWithLimitRule().diagnostics(
                """
                INSERT INTO player_snapshot(id, name)
                SELECT id, name
                FROM player
                OFFSET 5;
                """.asSqlDelightFile(),
                path = MIGRATION_SQM_PATH,
            )

        assertEquals(1, diagnostics.size)
        assertEquals(0, diagnostics.single().fixes.size)
    }

    @Test
    fun `accepts limit and offset with order by`() {
        RequireOrderByWithLimitRule().assertDiagnosticCount(
            """
            selectPlayers:
            SELECT id, name
            FROM player
            ORDER BY name
            LIMIT 10
            OFFSET 5;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `does not use subquery order by for outer limit`() {
        RequireOrderByWithLimitRule().assertDiagnosticCount(
            """
            selectPlayers:
            SELECT id
            FROM (
              SELECT id
              FROM player
              ORDER BY name
            )
            LIMIT 10;
            """.asSqlDelightFile(),
            1,
        )
    }

    @Test
    fun `does not use window order by for outer limit`() {
        RequireOrderByWithLimitRule().assertDiagnosticCount(
            """
            selectRanks:
            SELECT id, ROW_NUMBER() OVER (ORDER BY score DESC) AS rank
            FROM player
            LIMIT 10;
            """.asSqlDelightFile(),
            1,
        )
    }

    @Test
    fun `ignores nested limit and comments strings and quoted identifiers`() {
        RequireOrderByWithLimitRule().assertDiagnosticCount(
            """
            selectLiteral:
            -- SELECT id FROM player LIMIT 1
            SELECT 'LIMIT OFFSET ORDER BY', "LIMIT", `OFFSET`, [ORDER BY]
            FROM (
              SELECT id
              FROM player
              LIMIT 1
            ) AS limited_player;
            """.asSqlDelightFile(),
            0,
        )
    }
}
