package dev.s7a.sqldelight.check.rules.postgres.rules

import kotlin.test.Test

class NoConcurrentIndexInTransactionRuleTest {
    @Test
    fun `reports concurrent index in transaction`() {
        NoConcurrentIndexInTransactionRule().assertOne(
            """
            BEGIN;
            CREATE INDEX CONCURRENTLY player_name ON player(name);
            COMMIT;
            """,
        )
    }
}
