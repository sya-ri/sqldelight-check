package dev.s7a.sqldelight.check.rules.standard.rules

import kotlin.test.Test
import kotlin.test.assertEquals

class PreferExistsOverCountForExistenceRuleTest {
    @Test
    fun `reports count star greater than zero existence checks`() {
        val diagnostics =
            PreferExistsOverCountForExistenceRule().diagnostics(
                """
                hasPlayers:
                SELECT COUNT(*) > 0
                FROM player;
                """.asSqlDelightFile(),
            )

        assertEquals(1, diagnostics.size)
        assertEquals(0, diagnostics.single().fixes.size)
    }

    @Test
    fun `accepts real counts and exists`() {
        PreferExistsOverCountForExistenceRule().assertDiagnosticCount(
            """
            selectPlayerCount:
            SELECT COUNT(*)
            FROM player;

            hasPlayers:
            SELECT EXISTS(
              SELECT 1
              FROM player
            );
            """.asSqlDelightFile(),
            expected = 0,
        )
    }

    @Test
    fun `accepts other count comparisons`() {
        PreferExistsOverCountForExistenceRule().assertDiagnosticCount(
            """
            selectPopular:
            SELECT COUNT(*) >= 10
            FROM player;
            """.asSqlDelightFile(),
            expected = 0,
        )
    }

    @Test
    fun `ignores comments strings and quoted identifiers`() {
        PreferExistsOverCountForExistenceRule().assertDiagnosticCount(
            """
            selectLiteral:
            -- COUNT(*) > 0
            SELECT 'COUNT(*) > 0', "COUNT(*) > 0", `COUNT(*) > 0`, [COUNT(*) > 0]
            FROM player;
            """.asSqlDelightFile(),
            expected = 0,
        )
    }
}
