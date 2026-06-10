package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.FixSafety
import kotlin.test.Test
import kotlin.test.assertEquals

class LiteralCaseRuleTest {
    @Test
    fun `reports lowercase literals in sq named query`() {
        val diagnostics =
            LiteralCaseRule().diagnostics(
                """
                selectActive:
                SELECT id, name
                FROM player
                WHERE deleted_at IS null AND active = true;
                """.asSqlDelightFile(),
            )

        assertEquals(2, diagnostics.size)
        assertEquals(FixSafety.Unsafe, diagnostics.first().fixes.single().safety)
        assertEquals("NULL", diagnostics.first().fixes.single().edits.single().replacement)
    }

    @Test
    fun `reports false literal in migration defaults`() {
        LiteralCaseRule().assertDiagnosticCount(
            """
            ALTER TABLE player ADD COLUMN active BOOLEAN NOT NULL DEFAULT false;
            """.asSqlDelightFile(),
            1,
            path = MIGRATION_SQM_PATH,
        )
    }

    @Test
    fun `accepts uppercase literals`() {
        LiteralCaseRule().assertDiagnosticCount(
            """
            selectActive:
            SELECT id, name
            FROM player
            WHERE deleted_at IS NULL AND active = TRUE;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `ignores comments strings and quoted identifiers`() {
        LiteralCaseRule().assertDiagnosticCount(
            """
            selectLiteral:
            -- null true false
            SELECT 'null', "true", `false`, [null]
            FROM player;
            """.asSqlDelightFile(),
            0,
        )
    }
}
