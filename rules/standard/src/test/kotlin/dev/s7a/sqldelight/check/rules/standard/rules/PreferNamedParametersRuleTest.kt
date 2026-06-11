package dev.s7a.sqldelight.check.rules.standard.rules

import kotlin.test.Test
import kotlin.test.assertEquals

class PreferNamedParametersRuleTest {
    @Test
    fun `reports anonymous parameters in sq files`() {
        val diagnostics =
            PreferNamedParametersRule().diagnostics(
                """
                selectByNameAndScore:
                SELECT id, name
                FROM player
                WHERE name = ?
                  AND score > ?;
                """.asSqlDelightFile(),
            )

        assertEquals(2, diagnostics.size)
        assertEquals(0, diagnostics.first().fixes.size)
    }

    @Test
    fun `accepts named and variable arguments`() {
        PreferNamedParametersRule().assertDiagnosticCount(
            """
            selectByNames:
            SELECT id, name
            FROM player
            WHERE name = :name
               OR name IN ?;
            """.asSqlDelightFile(),
            expected = 0,
        )
    }

    @Test
    fun `ignores migration files`() {
        PreferNamedParametersRule().assertDiagnosticCount(
            """
            UPDATE player SET name = ?;
            """.asSqlDelightFile(),
            expected = 0,
            path = MIGRATION_SQM_PATH,
        )
    }

    @Test
    fun `ignores comments strings and quoted identifiers`() {
        PreferNamedParametersRule().assertDiagnosticCount(
            """
            selectLiteral:
            -- WHERE name = ?
            SELECT '?', "?", `?`, [?]
            FROM player;
            """.asSqlDelightFile(),
            expected = 0,
        )
    }
}
