package dev.s7a.sqldelight.check.rules.postgres.rules

import dev.s7a.sqldelight.check.api.DialectCapabilities
import dev.s7a.sqldelight.check.api.RuleId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for PostgreSQL REINDEX CONCURRENTLY diagnostics.
 */
class ReindexConcurrentlyRuleTest {
    @Test
    fun `reports reindex statements without concurrently`() {
        val diagnostics =
            ReindexConcurrentlyRule().diagnostics(
                """
                REINDEX INDEX player_name_idx;
                REINDEX TABLE player;
                """,
            )

        assertEquals(2, diagnostics.size)
        assertEquals(
            setOf(RuleId("postgres:reindex-concurrently")),
            diagnostics.map { diagnostic -> diagnostic.ruleId }.toSet(),
        )
        assertEquals(1, diagnostics.first().range?.start?.line)
        assertEquals(1, diagnostics.first().range?.start?.column)
        assertTrue(diagnostics.all { diagnostic -> diagnostic.fixes.isEmpty() })
    }

    @Test
    fun `does not report reindex concurrently`() {
        val diagnostics =
            ReindexConcurrentlyRule().diagnostics(
                """
                REINDEX INDEX CONCURRENTLY player_name_idx;
                REINDEX TABLE CONCURRENTLY player;
                """,
            )

        assertEquals(emptyList(), diagnostics)
    }

    @Test
    fun `does not report reindex system because concurrently is unsupported`() {
        val diagnostics =
            ReindexConcurrentlyRule().diagnostics(
                """
                REINDEX SYSTEM player_database;
                """,
            )

        assertEquals(emptyList(), diagnostics)
    }

    @Test
    fun `ignores comments strings and quoted identifiers`() {
        val diagnostics =
            ReindexConcurrentlyRule().diagnostics(
                """
                -- REINDEX INDEX player_name_idx;
                SELECT 'REINDEX TABLE player;';
                SELECT "REINDEX";
                SELECT `REINDEX`;
                SELECT [REINDEX];
                """,
            )

        assertEquals(emptyList(), diagnostics)
    }

    @Test
    fun `does not report for non postgres capabilities`() {
        val diagnostics =
            ReindexConcurrentlyRule().diagnostics(
                content =
                    """
                    REINDEX INDEX player_name_idx;
                    """,
                capabilities = setOf(DialectCapabilities.SQLite),
            )

        assertEquals(emptyList(), diagnostics)
    }
}
