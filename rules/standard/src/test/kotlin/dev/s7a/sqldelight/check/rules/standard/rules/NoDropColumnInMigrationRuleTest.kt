package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.Severity
import kotlin.test.Test
import kotlin.test.assertEquals

class NoDropColumnInMigrationRuleTest {
    @Test
    fun `reports drop column in migration files`() {
        val diagnostics =
            NoDropColumnInMigrationRule().diagnostics(
                """
                ALTER TABLE player DROP COLUMN score;
                """.asSqlDelightFile(),
                path = MIGRATION_SQM_PATH,
            )

        assertEquals(1, diagnostics.size)
        assertEquals(Severity.Error, diagnostics.single().severity)
        assertEquals("Avoid DROP COLUMN in SQLDelight migrations unless the destructive change is intentional.", diagnostics.single().message)
    }

    @Test
    fun `ignores sq files`() {
        NoDropColumnInMigrationRule().assertDiagnosticCount(
            """
            dropScore:
            ALTER TABLE player DROP COLUMN score;
            """.asSqlDelightFile(),
            expected = 0,
        )
    }

    @Test
    fun `ignores comments strings and quoted identifiers`() {
        NoDropColumnInMigrationRule().assertDiagnosticCount(
            """
            -- ALTER TABLE player DROP COLUMN score;
            SELECT 'DROP COLUMN', "drop", `column`, [column];
            """.asSqlDelightFile(),
            expected = 0,
            path = MIGRATION_SQM_PATH,
        )
    }
}
