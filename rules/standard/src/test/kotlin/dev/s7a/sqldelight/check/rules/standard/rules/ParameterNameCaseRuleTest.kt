package dev.s7a.sqldelight.check.rules.standard.rules

import kotlin.test.Test
import kotlin.test.assertEquals

class ParameterNameCaseRuleTest {
    @Test
    fun `reports named parameters that are not lower camel case`() {
        val diagnostics =
            ParameterNameCaseRule().diagnostics(
                """
                selectByName:
                SELECT id, name
                FROM player
                WHERE name = :PlayerName
                   OR name = :player_name;
                """.asSqlDelightFile(),
            )

        assertEquals(2, diagnostics.size)
        assertEquals("SQLDelight parameter ':PlayerName' should be lower camel case.", diagnostics.first().message)
        assertEquals(0, diagnostics.first().fixes.size)
    }

    @Test
    fun `accepts lower camel case parameters`() {
        ParameterNameCaseRule().assertDiagnosticCount(
            """
            selectByName:
            SELECT id, name
            FROM player
            WHERE name = :playerName;
            """.asSqlDelightFile(),
            expected = 0,
        )
    }

    @Test
    fun `ignores migrations comments strings and casts`() {
        ParameterNameCaseRule().assertDiagnosticCount(
            """
            -- :PlayerName
            SELECT ':PlayerName', value::TEXT
            FROM player
            WHERE name = :PlayerName;
            """.asSqlDelightFile(),
            expected = 0,
            path = MIGRATION_SQM_PATH,
        )
    }
}
