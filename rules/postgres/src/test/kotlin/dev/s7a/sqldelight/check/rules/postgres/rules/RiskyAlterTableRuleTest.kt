package dev.s7a.sqldelight.check.rules.postgres.rules

import dev.s7a.sqldelight.check.api.QualifiedRuleId


import dev.s7a.sqldelight.check.api.DialectCapability
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.RuleSetId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for risky PostgreSQL ALTER TABLE diagnostics.
 */
class RiskyAlterTableRuleTest {
    @Test
    fun `reports risky alter table operations`() {
        val diagnostics =
            RiskyAlterTableRule().diagnostics(
                """
                ALTER TABLE player ALTER COLUMN name TYPE text;
                ALTER TABLE player ALTER COLUMN email SET NOT NULL;
                ALTER TABLE player DROP COLUMN legacy_name;
                ALTER TABLE player ADD CONSTRAINT player_name_check CHECK (name <> '');
                """,
            )

        assertEquals(4, diagnostics.size)
        assertEquals(
            setOf(qualifiedRuleId("postgres:risky-alter-table")),
            diagnostics.map { diagnostic -> diagnostic.ruleId }.toSet(),
        )
        assertEquals(1, diagnostics.first().range?.start?.line)
        assertEquals(1, diagnostics.first().range?.start?.column)
        assertTrue(diagnostics.all { diagnostic -> diagnostic.fixes.isEmpty() })
    }

    @Test
    fun `does not report add constraint not valid`() {
        val diagnostics =
            RiskyAlterTableRule().diagnostics(
                """
                ALTER TABLE player
                  ADD CONSTRAINT player_name_check CHECK (name <> '') NOT VALID;
                """,
            )

        assertEquals(emptyList(), diagnostics)
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
                SELECT 'ALTER TABLE player ALTER COLUMN name TYPE text;';
                SELECT "ALTER TABLE";
                SELECT `ALTER TABLE`;
                SELECT [ALTER TABLE];
                """,
            )

        assertEquals(emptyList(), diagnostics)
    }

    @Test
    fun `does not report for non postgres capabilities`() {
        val diagnostics =
            RiskyAlterTableRule().diagnostics(
                content =
                    """
                    ALTER TABLE player DROP COLUMN legacy_name;
                    """,
                capabilities = setOf(DialectCapability.SQLite),
            )

        assertEquals(emptyList(), diagnostics)
    }
}

private fun qualifiedRuleId(value: String): QualifiedRuleId {
    val delimiter = value.indexOf(':')
    require(delimiter > 0 && delimiter < value.lastIndex)
    return QualifiedRuleId(
        ruleSetId = RuleSetId(value.substring(0, delimiter)),
        ruleId = RuleId(value.substring(delimiter + 1)),
    )
}
