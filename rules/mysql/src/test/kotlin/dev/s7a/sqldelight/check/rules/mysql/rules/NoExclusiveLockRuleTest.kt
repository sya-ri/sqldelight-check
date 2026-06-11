package dev.s7a.sqldelight.check.rules.mysql.rules

import kotlin.test.Test

class NoExclusiveLockRuleTest {
    @Test
    fun `reports exclusive lock`() {
        NoExclusiveLockRule().assertOne("ALTER TABLE player ADD COLUMN score INT, LOCK=EXCLUSIVE;")
    }
}
