package dev.s7a.sqldelight.check.rules.postgres.rules

import kotlin.test.Test
import kotlin.test.assertEquals

class NoDropIndexNonConcurrentlyRuleTest {
    @Test
    fun `reports drop index without concurrently`() {
        NoDropIndexNonConcurrentlyRule().assertOne(
            """
            DROP INDEX player_name_idx;
            """,
        )
    }

    @Test
    fun `ignores drop index concurrently`() {
        assertEquals(
            emptyList(),
            NoDropIndexNonConcurrentlyRule().diagnostics(
                """
                DROP INDEX CONCURRENTLY player_name_idx;
                """,
            ),
        )
    }
}
