package dev.s7a.sqldelight.check.rules.standard.rules

import kotlin.test.Test
import kotlin.test.assertEquals

class OrderByTargetNewlineRuleTest {
    @Test
    fun `reports multiline order by list with multiple targets on one line`() {
        val diagnostics =
            OrderByTargetNewlineRule().diagnostics(
                """
                selectPlayers:
                SELECT id, name, age
                FROM player
                ORDER BY name, age,
                  id;
                """.asSqlDelightFile(),
            )

        assertEquals(1, diagnostics.size)
        assertEquals(0, diagnostics.single().fixes.size)
    }

    @Test
    fun `accepts one order by target per line`() {
        OrderByTargetNewlineRule().assertDiagnosticCount(
            """
            selectPlayers:
            SELECT id, name, age
            FROM player
            ORDER BY
              name,
              age,
              id;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `accepts single line order by lists`() {
        OrderByTargetNewlineRule().assertDiagnosticCount(
            """
            selectPlayers:
            SELECT id, name, age FROM player ORDER BY name, age;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `ignores commas in nested expressions`() {
        OrderByTargetNewlineRule().assertDiagnosticCount(
            """
            selectPlayers:
            SELECT id, name, age
            FROM player
            ORDER BY
              COALESCE(name, 'missing'),
              age;
            """.asSqlDelightFile(),
            0,
        )
    }
}
