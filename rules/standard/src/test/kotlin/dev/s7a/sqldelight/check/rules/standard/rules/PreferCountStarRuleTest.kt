package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.FixSafety
import kotlin.test.Test
import kotlin.test.assertEquals

class PreferCountStarRuleTest {
    @Test
    fun `reports unsafe fix for count one and count zero`() {
        val diagnostics =
            PreferCountStarRule().diagnostics(
                """
                selectPlayerCounts:
                SELECT COUNT(1), COUNT(0)
                FROM player;
                """.asSqlDelightFile(),
            )

        assertEquals(2, diagnostics.size)
        assertEquals(FixSafety.Unsafe, diagnostics.first().fixes.single().safety)
        assertEquals("*", diagnostics.first().fixes.single().edits.single().replacement)
    }

    @Test
    fun `reports count row syntax in migration files`() {
        val diagnostics =
            PreferCountStarRule().diagnostics(
                """
                INSERT INTO player_count_snapshot(total)
                SELECT COUNT(1)
                FROM player;
                """.asSqlDelightFile(),
                path = MIGRATION_SQM_PATH,
            )

        assertEquals(1, diagnostics.size)
    }

    @Test
    fun `accepts count star and real count expressions`() {
        PreferCountStarRule().assertDiagnosticCount(
            """
            selectPlayerCounts:
            SELECT COUNT(*), COUNT(score), COUNT(DISTINCT name), SUM(1)
            FROM player;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `accepts spaced count star`() {
        PreferCountStarRule().assertDiagnosticCount(
            """
            selectPlayerCounts:
            SELECT COUNT( * )
            FROM player;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `ignores comments strings and quoted identifiers`() {
        PreferCountStarRule().assertDiagnosticCount(
            """
            selectLiteral:
            -- COUNT(1)
            SELECT 'COUNT(1)', "COUNT(1)", `COUNT(1)`, [COUNT(1)]
            FROM player;
            """.asSqlDelightFile(),
            0,
        )
    }
}
