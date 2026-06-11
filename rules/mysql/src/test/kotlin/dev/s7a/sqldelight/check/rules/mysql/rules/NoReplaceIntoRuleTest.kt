package dev.s7a.sqldelight.check.rules.mysql.rules

import dev.s7a.sqldelight.check.api.QualifiedRuleId


import dev.s7a.sqldelight.check.api.DialectCapability
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.RuleSetId
import dev.s7a.sqldelight.check.api.Severity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for MySQL REPLACE INTO diagnostics.
 */
class NoReplaceIntoRuleTest {
    @Test
    fun `reports replace into statements`() {
        val diagnostics =
            NoReplaceIntoRule().diagnostics(
                """
                replacePlayer:
                REPLACE INTO player(id, name)
                VALUES (?, ?);
                """,
            )

        assertEquals(1, diagnostics.size)
        assertEquals(qualifiedRuleId("mysql:no-replace-into"), diagnostics.single().ruleId)
        assertEquals(Severity.Error, diagnostics.single().severity)
        assertEquals(2, diagnostics.single().range?.start?.line)
        assertEquals(1, diagnostics.single().range?.start?.column)
        assertTrue(diagnostics.single().fixes.isEmpty())
    }

    @Test
    fun `does not report replace used without into`() {
        val diagnostics =
            NoReplaceIntoRule().diagnostics(
                """
                selectReplacement:
                SELECT REPLACE(name, 'a', 'b')
                FROM player;
                """,
            )

        assertEquals(emptyList(), diagnostics)
    }

    @Test
    fun `ignores comments strings and quoted identifiers`() {
        val diagnostics =
            NoReplaceIntoRule().diagnostics(
                """
                -- REPLACE INTO player(id) VALUES (1);
                # REPLACE INTO player(id) VALUES (1);
                SELECT 'REPLACE INTO player(id) VALUES (1);';
                SELECT "REPLACE INTO player(id) VALUES (1);";
                SELECT `REPLACE`;
                SELECT [REPLACE];
                """,
            )

        assertEquals(emptyList(), diagnostics)
    }

    @Test
    fun `does not report for non mysql capabilities`() {
        val diagnostics =
            NoReplaceIntoRule().diagnostics(
                content =
                    """
                    REPLACE INTO player(id, name) VALUES (1, 'Ada');
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
