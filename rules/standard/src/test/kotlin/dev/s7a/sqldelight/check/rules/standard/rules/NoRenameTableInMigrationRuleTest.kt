package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.Severity
import kotlin.test.Test
import kotlin.test.assertEquals

class NoRenameTableInMigrationRuleTest {
    @Test
    fun `reports alter table rename to in migration files`() {
        val diagnostics =
            NoRenameTableInMigrationRule().diagnostics(
                """
                ALTER TABLE player RENAME TO user_player;
                """.asSqlDelightFile(),
                path = MIGRATION_SQM_PATH,
            )

        assertEquals(1, diagnostics.size)
        assertEquals(Severity.Error, diagnostics.single().severity)
        assertEquals(
            "Avoid table renames in SQLDelight migrations because they can break live application compatibility.",
            diagnostics.single().message,
        )
    }

    @Test
    fun `ignores sq files`() {
        NoRenameTableInMigrationRule().assertDiagnosticCount(
            """
            renamePlayer:
            ALTER TABLE player RENAME TO user_player;
            """.asSqlDelightFile(),
            expected = 0,
        )
    }

    @Test
    fun `ignores comments strings and quoted identifiers`() {
        NoRenameTableInMigrationRule().assertDiagnosticCount(
            """
            -- ALTER TABLE player RENAME TO user_player;
            SELECT 'ALTER TABLE player RENAME TO user_player', "rename", `table`, [table];
            """.asSqlDelightFile(),
            expected = 0,
            path = MIGRATION_SQM_PATH,
        )
    }
}
