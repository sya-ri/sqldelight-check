package dev.s7a.sqldelight.check.rules.standard.rules

import kotlin.test.Test
import kotlin.test.assertEquals

class NoUnusedCteRuleTest {
    @Test
    fun `reports unused ctes`() {
        val diagnostics =
            NoUnusedCteRule().diagnostics(
                """
                selectPlayers:
                WITH ranked AS (
                  SELECT id
                  FROM player
                )
                SELECT id
                FROM player;
                """.asSqlDelightFile(),
            )

        assertEquals(1, diagnostics.size)
        assertEquals("CTE 'ranked' is not referenced by the main query.", diagnostics.single().message)
        assertEquals(0, diagnostics.single().fixes.size)
    }

    @Test
    fun `accepts referenced ctes`() {
        NoUnusedCteRule().assertDiagnosticCount(
            """
            selectPlayers:
            WITH ranked AS (
              SELECT id
              FROM player
            )
            SELECT id
            FROM ranked;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `reports only unused ctes in a multi cte clause`() {
        NoUnusedCteRule().assertDiagnosticCount(
            """
            selectPlayers:
            WITH ranked AS (
              SELECT id
              FROM player
            ), unused AS (
              SELECT id
              FROM player
            )
            SELECT id
            FROM ranked;
            """.asSqlDelightFile(),
            1,
        )
    }

    @Test
    fun `ignores comments strings and nested with clauses`() {
        NoUnusedCteRule().assertDiagnosticCount(
            """
            selectPlayers:
            -- WITH unused AS (SELECT id FROM player)
            SELECT 'WITH unused AS (SELECT id FROM player)'
            FROM player
            WHERE EXISTS (
              WITH nested AS (SELECT id FROM team)
              SELECT id FROM nested
            );
            """.asSqlDelightFile(),
            0,
        )
    }
}
