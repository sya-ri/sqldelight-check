package dev.s7a.sqldelight.check.rules.postgres.rules

import kotlin.test.Test

class NoDropColumnRuleTest {
    @Test
    fun `reports drop column`() {
        NoDropColumnRule().assertOne("ALTER TABLE player DROP COLUMN old_name;")
    }
}
