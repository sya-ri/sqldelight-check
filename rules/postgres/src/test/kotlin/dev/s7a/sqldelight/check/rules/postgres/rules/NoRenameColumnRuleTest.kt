package dev.s7a.sqldelight.check.rules.postgres.rules

import kotlin.test.Test

class NoRenameColumnRuleTest {
    @Test
    fun `reports rename column`() {
        NoRenameColumnRule().assertOne("ALTER TABLE player RENAME COLUMN name TO display_name;")
    }
}
