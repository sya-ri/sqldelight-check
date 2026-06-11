package dev.s7a.sqldelight.check.rules.postgres.rules

import kotlin.test.Test

class NoRenameTableRuleTest {
    @Test
    fun `reports rename table`() {
        NoRenameTableRule().assertOne("ALTER TABLE player RENAME TO players;")
    }
}
