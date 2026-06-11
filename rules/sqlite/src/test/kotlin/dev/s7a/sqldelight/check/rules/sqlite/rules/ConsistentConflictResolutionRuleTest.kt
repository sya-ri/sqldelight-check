package dev.s7a.sqldelight.check.rules.sqlite.rules

import dev.s7a.sqldelight.check.api.QualifiedRuleId


import dev.s7a.sqldelight.check.api.DialectCapabilities
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.RuleSetId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for SQLite conflict-resolution consistency diagnostics.
 */
class ConsistentConflictResolutionRuleTest {
    @Test
    fun `reports mixed conflict resolution styles`() {
        val diagnostics =
            ConsistentConflictResolutionRule().diagnostics(
                content =
                    """
                    upsertPlayer:
                    INSERT OR REPLACE INTO player(id, name)
                    VALUES (?, ?);

                    replaceTeam:
                    REPLACE INTO team(id, name)
                    VALUES (?, ?);

                    upsertClub:
                    INSERT INTO club(id, name)
                    VALUES (?, ?)
                    ON CONFLICT(id) DO UPDATE SET name = excluded.name;
                    """,
                path = "src/main/sqldelight/com/example/Player.sq",
            )

        assertEquals(3, diagnostics.size)
        assertEquals(
            setOf(qualifiedRuleId("sqlite:consistent-conflict-resolution")),
            diagnostics.map { diagnostic -> diagnostic.ruleId }.toSet(),
        )
        assertEquals(2, diagnostics.first().range?.start?.line)
        assertEquals(1, diagnostics.first().range?.start?.column)
        assertTrue(diagnostics.all { diagnostic -> diagnostic.fixes.isEmpty() })
    }

    @Test
    fun `does not report one conflict resolution style`() {
        val diagnostics =
            ConsistentConflictResolutionRule().diagnostics(
                content =
                    """
                    upsertPlayer:
                    INSERT OR REPLACE INTO player(id, name)
                    VALUES (?, ?);

                    upsertTeam:
                    INSERT OR REPLACE INTO team(id, name)
                    VALUES (?, ?);
                    """,
                path = "src/main/sqldelight/com/example/Player.sq",
            )

        assertEquals(emptyList(), diagnostics)
    }

    @Test
    fun `ignores comments strings and quoted identifiers`() {
        val diagnostics =
            ConsistentConflictResolutionRule().diagnostics(
                content =
                    """
                    -- INSERT OR REPLACE INTO player(id, name) VALUES (1, 'Ada');
                    SELECT 'REPLACE INTO player(id, name) VALUES (1, ''Ada'');';
                    SELECT "ON CONFLICT(id) DO UPDATE";
                    SELECT `REPLACE INTO`;
                    SELECT [INSERT OR REPLACE];
                    """,
                path = "src/main/sqldelight/com/example/Player.sq",
            )

        assertEquals(emptyList(), diagnostics)
    }

    @Test
    fun `does not report for non sqlite capabilities`() {
        val diagnostics =
            ConsistentConflictResolutionRule().diagnostics(
                content =
                    """
                    INSERT OR REPLACE INTO player(id, name) VALUES (1, 'Ada');
                    REPLACE INTO team(id, name) VALUES (1, 'Core');
                    """,
                capabilities = setOf(DialectCapabilities.PostgreSql),
                path = "src/main/sqldelight/com/example/Player.sq",
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
