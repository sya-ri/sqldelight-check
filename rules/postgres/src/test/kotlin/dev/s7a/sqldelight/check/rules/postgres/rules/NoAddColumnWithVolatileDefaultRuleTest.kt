package dev.s7a.sqldelight.check.rules.postgres.rules

import kotlin.test.Test

class NoAddColumnWithVolatileDefaultRuleTest {
    @Test
    fun `reports add column with volatile default`() {
        NoAddColumnWithVolatileDefaultRule().assertOne(
            "ALTER TABLE player ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT now();",
        )
    }
}
