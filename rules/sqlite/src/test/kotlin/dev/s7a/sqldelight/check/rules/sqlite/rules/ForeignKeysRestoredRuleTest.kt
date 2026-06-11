package dev.s7a.sqldelight.check.rules.sqlite.rules

import dev.s7a.sqldelight.check.api.QualifiedRuleId


import dev.s7a.sqldelight.check.api.DialectCapabilities
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.RuleSetId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for SQLite foreign key restoration diagnostics.
 */
class ForeignKeysRestoredRuleTest {
    @Test
    fun `reports foreign keys disabled without later restore in migration`() {
        val diagnostics =
            ForeignKeysRestoredRule().diagnostics(
                """
                PRAGMA foreign_keys = OFF;

                DROP TABLE player;
                """,
            )

        assertEquals(1, diagnostics.size)
        assertEquals(qualifiedRuleId("sqlite:foreign-keys-restored"), diagnostics.single().qualifiedRuleId)
        assertEquals(1, diagnostics.single().range?.start?.line)
        assertEquals(1, diagnostics.single().range?.start?.column)
        assertTrue(diagnostics.single().fixes.isEmpty())
    }

    @Test
    fun `does not report foreign keys disabled with later restore`() {
        val diagnostics =
            ForeignKeysRestoredRule().diagnostics(
                """
                PRAGMA foreign_keys = OFF;

                DROP TABLE player;

                PRAGMA foreign_keys = ON;
                """,
            )

        assertEquals(emptyList(), diagnostics)
    }

    @Test
    fun `does not report sq files`() {
        val diagnostics =
            ForeignKeysRestoredRule().diagnostics(
                content =
                    """
                    PRAGMA foreign_keys = OFF;
                    """,
                path = "src/main/sqldelight/com/example/Player.sq",
            )

        assertEquals(emptyList(), diagnostics)
    }

    @Test
    fun `ignores comments strings and quoted identifiers`() {
        val diagnostics =
            ForeignKeysRestoredRule().diagnostics(
                """
                -- PRAGMA foreign_keys = OFF;
                SELECT 'PRAGMA foreign_keys = OFF;';
                SELECT "PRAGMA";
                SELECT `PRAGMA`;
                SELECT [PRAGMA];
                """,
            )

        assertEquals(emptyList(), diagnostics)
    }

    @Test
    fun `does not report for non sqlite capabilities`() {
        val diagnostics =
            ForeignKeysRestoredRule().diagnostics(
                content =
                    """
                    PRAGMA foreign_keys = OFF;
                    """,
                capabilities = setOf(DialectCapabilities.PostgreSql),
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
