package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.FixSafety
import kotlin.test.Test
import kotlin.test.assertEquals

class PreferCoalesceRuleTest {
    @Test
    fun `reports unsafe fix for ifnull and nvl`() {
        val diagnostics =
            PreferCoalesceRule().diagnostics(
                """
                selectDisplayName:
                SELECT IFNULL(nickname, name), NVL(display_name, name)
                FROM player;
                """.asSqlDelightFile(),
            )

        assertEquals(2, diagnostics.size)
        assertEquals(FixSafety.Unsafe, diagnostics.first().fixes.single().safety)
        assertEquals("COALESCE", diagnostics.first().fixes.single().edits.single().replacement)
        PreferCoalesceRule().assertAllFixes(
            """
            selectDisplayName:
            SELECT IFNULL(nickname, name), NVL(display_name, name)
            FROM player;
            """.asSqlDelightFile(),
            """
            selectDisplayName:
            SELECT COALESCE(nickname, name), COALESCE(display_name, name)
            FROM player;
            """.asSqlDelightFile(),
        )
    }

    @Test
    fun `reports coalesce alternatives in migration files`() {
        val diagnostics =
            PreferCoalesceRule().diagnostics(
                """
                UPDATE player
                SET nickname = IFNULL(nickname, name);
                """.asSqlDelightFile(),
                path = MIGRATION_SQM_PATH,
            )

        assertEquals(1, diagnostics.size)
    }

    @Test
    fun `accepts coalesce and non call identifiers`() {
        PreferCoalesceRule().assertDiagnosticCount(
            """
            selectDisplayName:
            SELECT COALESCE(nickname, name), ifnull
            FROM player;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `ignores comments strings and quoted identifiers`() {
        PreferCoalesceRule().assertDiagnosticCount(
            """
            selectLiteral:
            -- IFNULL(name, 'unknown')
            SELECT 'IFNULL(name)', "IFNULL", `IFNULL`, [IFNULL]
            FROM player;
            """.asSqlDelightFile(),
            0,
        )
    }
}
