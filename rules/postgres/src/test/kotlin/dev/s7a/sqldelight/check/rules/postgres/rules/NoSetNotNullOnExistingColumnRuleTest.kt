package dev.s7a.sqldelight.check.rules.postgres.rules

import kotlin.test.Test

class NoSetNotNullOnExistingColumnRuleTest {
    @Test
    fun `reports set not null on existing column`() {
        NoSetNotNullOnExistingColumnRule().assertOne(
            "ALTER TABLE player ALTER COLUMN name SET NOT NULL;",
        )
    }
}
