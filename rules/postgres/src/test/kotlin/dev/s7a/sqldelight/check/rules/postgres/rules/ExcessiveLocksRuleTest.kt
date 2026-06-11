package dev.s7a.sqldelight.check.rules.postgres.rules

import dev.s7a.sqldelight.check.api.DialectCapabilities
import dev.s7a.sqldelight.check.api.RuleId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for excessive PostgreSQL lock diagnostics.
 */
class ExcessiveLocksRuleTest {
    @Test
    fun `reports create index without concurrently`() {
        val diagnostics =
            ExcessiveLocksRule().diagnostics(
                """
                CREATE INDEX player_name ON player(name);
                """,
            )

        assertEquals(1, diagnostics.size)
        assertEquals(RuleId("postgres:excessive-locks"), diagnostics.single().ruleId)
        assertEquals(1, diagnostics.single().range?.start?.line)
        assertEquals(1, diagnostics.single().range?.start?.column)
        assertTrue(diagnostics.single().fixes.isEmpty())
    }

    @Test
    fun `does not report create index concurrently`() {
        val diagnostics =
            ExcessiveLocksRule().diagnostics(
                """
                CREATE INDEX CONCURRENTLY player_name ON player(name);
                """,
            )

        assertEquals(emptyList(), diagnostics)
    }

    @Test
    fun `reports unique create index without concurrently`() {
        val diagnostics =
            ExcessiveLocksRule().diagnostics(
                """
                CREATE UNIQUE INDEX player_name ON player(name);
                """,
            )

        assertEquals(1, diagnostics.size)
    }

    @Test
    fun `ignores comments and quoted strings`() {
        val diagnostics =
            ExcessiveLocksRule().diagnostics(
                """
                -- CREATE INDEX player_name ON player(name);
                SELECT 'CREATE INDEX player_name ON player(name);';
                """,
            )

        assertEquals(emptyList(), diagnostics)
    }

    @Test
    fun `does not report for non postgres capabilities`() {
        val diagnostics =
            ExcessiveLocksRule().diagnostics(
                content =
                    """
                    CREATE INDEX player_name ON player(name);
                    """,
                capabilities = setOf(DialectCapabilities.SQLite),
            )

        assertEquals(emptyList(), diagnostics)
    }
}
