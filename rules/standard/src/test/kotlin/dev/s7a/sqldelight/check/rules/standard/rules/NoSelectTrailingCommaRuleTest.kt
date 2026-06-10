package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.FixSafety
import kotlin.test.Test
import kotlin.test.assertEquals

class NoSelectTrailingCommaRuleTest {
    @Test
    fun `reports unsafe fix for trailing select comma`() {
        val diagnostics =
            NoSelectTrailingCommaRule().diagnostics(
                """
                selectPlayers:
                SELECT id, name,
                FROM player;
                """.asSqlDelightFile(),
            )

        assertEquals(1, diagnostics.size)
        assertEquals(FixSafety.Unsafe, diagnostics.single().fixes.single().safety)
        assertEquals("", diagnostics.single().fixes.single().edits.single().replacement)
    }

    @Test
    fun `reports trailing select comma in migration files`() {
        val diagnostics =
            NoSelectTrailingCommaRule().diagnostics(
                """
                INSERT INTO player_snapshot(id, name)
                SELECT id, name,
                FROM player;
                """.asSqlDelightFile(),
                path = MIGRATION_SQM_PATH,
            )

        assertEquals(1, diagnostics.size)
    }

    @Test
    fun `accepts comma separated select list`() {
        NoSelectTrailingCommaRule().assertDiagnosticCount(
            """
            selectPlayers:
            SELECT id, name
            FROM player;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `ignores comments strings and quoted identifiers`() {
        NoSelectTrailingCommaRule().assertDiagnosticCount(
            """
            selectLiteral:
            -- SELECT id, name, FROM player
            SELECT 'id, name, FROM player', "id, name,", `name,`, [name,]
            FROM player;
            """.asSqlDelightFile(),
            0,
        )
    }
}
