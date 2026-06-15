package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.Severity
import kotlin.test.Test
import kotlin.test.assertEquals

class NoRenameColumnInMigrationRuleTest {
    @Test
    fun `reports rename column in migration files`() {
        val diagnostics =
            NoRenameColumnInMigrationRule().diagnostics(
                """
                ALTER TABLE player RENAME COLUMN score TO points;
                """.asSqlDelightFile(),
                path = MIGRATION_SQM_PATH,
            )

        assertEquals(1, diagnostics.size)
        assertEquals(Severity.Error, diagnostics.single().severity)
        assertEquals(
            "Avoid RENAME COLUMN in SQLDelight migrations because it can break live application compatibility.",
            diagnostics.single().message,
        )
    }

    @Test
    fun `ignores sq files`() {
        NoRenameColumnInMigrationRule().assertDiagnosticCount(
            """
            renameScore:
            ALTER TABLE player RENAME COLUMN score TO points;
            """.asSqlDelightFile(),
            expected = 0,
        )
    }

    @Test
    fun `ignores comments strings and quoted identifiers`() {
        NoRenameColumnInMigrationRule().assertDiagnosticCount(
            """
            -- ALTER TABLE player RENAME COLUMN score TO points;
            SELECT 'RENAME COLUMN', "rename", `column`, [column];
            """.asSqlDelightFile(),
            expected = 0,
            path = MIGRATION_SQM_PATH,
        )
    }
}
