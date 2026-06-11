package dev.s7a.sqldelight.check.rules.standard.rules

import kotlin.test.Test
import kotlin.test.assertEquals

class NoDropTableInMigrationRuleTest {
    @Test
    fun `reports drop table in migration files`() {
        val diagnostics =
            NoDropTableInMigrationRule().diagnostics(
                """
                DROP TABLE player;
                """.asSqlDelightFile(),
                path = MIGRATION_SQM_PATH,
            )

        assertEquals(1, diagnostics.size)
        assertEquals("Avoid DROP TABLE in SQLDelight migrations unless the destructive change is intentional.", diagnostics.single().message)
    }

    @Test
    fun `ignores sq files`() {
        NoDropTableInMigrationRule().assertDiagnosticCount(
            """
            dropPlayer:
            DROP TABLE player;
            """.asSqlDelightFile(),
            expected = 0,
        )
    }

    @Test
    fun `ignores comments strings and quoted identifiers`() {
        NoDropTableInMigrationRule().assertDiagnosticCount(
            """
            -- DROP TABLE player;
            SELECT 'DROP TABLE', "drop", `table`, [table];
            """.asSqlDelightFile(),
            expected = 0,
            path = MIGRATION_SQM_PATH,
        )
    }
}
