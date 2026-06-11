package dev.s7a.sqldelight.check.rules.sqlite.rules

import kotlin.test.Test

class NoAlterTableComplexChangeRuleTest {
    @Test
    fun `reports complex alter table changes`() {
        NoAlterTableComplexChangeRule().assertOne("ALTER TABLE player ADD CONSTRAINT player_name_unique UNIQUE (name);")
    }
}
