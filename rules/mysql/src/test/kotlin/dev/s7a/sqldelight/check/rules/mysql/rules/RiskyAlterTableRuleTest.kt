package dev.s7a.sqldelight.check.rules.mysql.rules

import dev.s7a.sqldelight.check.api.DialectCapabilities
import dev.s7a.sqldelight.check.api.RuleId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for risky MySQL ALTER TABLE diagnostics.
 */
class RiskyAlterTableRuleTest {
    @Test
    fun `reports risky alter table operations`() {
        val diagnostics =
            RiskyAlterTableRule().diagnostics(
                """
                ALTER TABLE player MODIFY COLUMN name VARCHAR(255);
                ALTER TABLE player CHANGE COLUMN nickname display_name VARCHAR(255);
                ALTER TABLE player DROP COLUMN legacy_name;
                """,
            )

        assertEquals(3, diagnostics.size)
        assertEquals(
            setOf(RuleId("mysql:risky-alter-table")),
            diagnostics.map { diagnostic -> diagnostic.ruleId }.toSet(),
        )
        assertEquals(1, diagnostics.first().range?.start?.line)
        assertEquals(1, diagnostics.first().range?.start?.column)
        assertTrue(diagnostics.all { diagnostic -> diagnostic.fixes.isEmpty() })
    }

    @Test
    fun `does not report lower-risk alter table statements`() {
        val diagnostics =
            RiskyAlterTableRule().diagnostics(
                """
                ALTER TABLE player RENAME COLUMN name TO display_name;
                ALTER TABLE player ADD COLUMN created_at TEXT;
                """,
            )

        assertEquals(emptyList(), diagnostics)
    }

    @Test
    fun `ignores comments strings and quoted identifiers`() {
        val diagnostics =
            RiskyAlterTableRule().diagnostics(
                """
                -- ALTER TABLE player DROP COLUMN legacy_name;
                # ALTER TABLE player MODIFY COLUMN name TEXT;
                SELECT 'ALTER TABLE player CHANGE COLUMN old_name name TEXT;';
                SELECT "ALTER TABLE player DROP COLUMN legacy_name;";
                SELECT `ALTER TABLE`;
                SELECT [ALTER TABLE];
                """,
            )

        assertEquals(emptyList(), diagnostics)
    }

    @Test
    fun `does not report for non mysql capabilities`() {
        val diagnostics =
            RiskyAlterTableRule().diagnostics(
                content =
                    """
                    ALTER TABLE player DROP COLUMN legacy_name;
                    """,
                capabilities = setOf(DialectCapabilities.SQLite),
            )

        assertEquals(emptyList(), diagnostics)
    }
}
