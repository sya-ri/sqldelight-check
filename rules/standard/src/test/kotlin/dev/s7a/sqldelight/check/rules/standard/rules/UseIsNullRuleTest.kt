package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.FixSafety
import kotlin.test.Test
import kotlin.test.assertEquals

class UseIsNullRuleTest {
    @Test
    fun `reports unsafe fix for equals null`() {
        val diagnostics =
            UseIsNullRule().diagnostics(
                """
                selectMissingName:
                SELECT id, name
                FROM player
                WHERE name = NULL;
                """.asSqlDelightFile(),
            )

        assertEquals(1, diagnostics.size)
        assertEquals(FixSafety.Unsafe, diagnostics.single().fixes.single().safety)
        assertEquals("IS", diagnostics.single().fixes.single().edits.single().replacement)
    }

    @Test
    fun `reports unsafe fix for not equals null`() {
        val diagnostics =
            UseIsNullRule().diagnostics(
                """
                selectNamedPlayers:
                SELECT id, name
                FROM player
                WHERE name != NULL OR nickname <> NULL;
                """.asSqlDelightFile(),
            )

        assertEquals(2, diagnostics.size)
        assertEquals("IS NOT", diagnostics.first().fixes.single().edits.single().replacement)
    }

    @Test
    fun `reports null comparison in migration files`() {
        val diagnostics =
            UseIsNullRule().diagnostics(
                """
                DELETE FROM player
                WHERE deleted_at = NULL;
                """.asSqlDelightFile(),
                path = MIGRATION_SQM_PATH,
            )

        assertEquals(1, diagnostics.size)
    }

    @Test
    fun `accepts is null and set clause null assignments`() {
        UseIsNullRule().assertDiagnosticCount(
            """
            updateDeletedAt:
            UPDATE player
            SET deleted_at = NULL, nickname = NULL
            WHERE id IS NULL OR name IS NOT NULL;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `ignores comments strings and quoted identifiers`() {
        UseIsNullRule().assertDiagnosticCount(
            """
            selectLiteral:
            -- name = NULL
            SELECT 'name = NULL', "name = NULL", `name = NULL`, [name = NULL]
            FROM player;
            """.asSqlDelightFile(),
            0,
        )
    }
}
