package dev.s7a.sqldelight.check.rules.mysql.rules

import dev.s7a.sqldelight.check.api.DialectId
import dev.s7a.sqldelight.check.api.QualifiedRuleId
import dev.s7a.sqldelight.check.api.Severity
import kotlin.test.Test
import kotlin.test.assertEquals

class NoRenameTableInMigrationRuleTest {
    @Test
    fun `reports mysql rename table in migration files`() {
        val diagnostics =
            NoRenameTableInMigrationRule().diagnostics(
                """
                RENAME TABLE player TO user_player;
                """,
            )

        assertEquals(1, diagnostics.size)
        assertEquals(QualifiedRuleId("mysql:no-rename-table-in-migration"), diagnostics.single().ruleId)
        assertEquals(Severity.Error, diagnostics.single().severity)
        assertEquals(
            "Avoid table renames in SQLDelight migrations because they can break live application compatibility.",
            diagnostics.single().message,
        )
    }

    @Test
    fun `ignores sq files`() {
        val diagnostics =
            NoRenameTableInMigrationRule().diagnostics(
                """
                renamePlayer:
                RENAME TABLE player TO user_player;
                """,
                path = "src/main/sqldelight/com/example/Query.sq",
            )

        assertEquals(emptyList(), diagnostics)
    }

    @Test
    fun `ignores comments strings and quoted identifiers`() {
        val diagnostics =
            NoRenameTableInMigrationRule().diagnostics(
                """
                -- RENAME TABLE player TO user_player;
                # RENAME TABLE player TO user_player;
                SELECT 'RENAME TABLE player TO user_player';
                SELECT "RENAME TABLE player TO user_player";
                SELECT `RENAME`;
                SELECT [RENAME];
                """,
            )

        assertEquals(emptyList(), diagnostics)
    }

    @Test
    fun `does not report for non mysql dialect IDs`() {
        val diagnostics =
            NoRenameTableInMigrationRule().diagnostics(
                content =
                    """
                    RENAME TABLE player TO user_player;
                    """,
                ids = setOf(DialectId("other")),
            )

        assertEquals(emptyList(), diagnostics)
    }
}
