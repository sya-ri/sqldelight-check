package dev.s7a.sqldelight.check.rules.sqlite.rules

import kotlin.test.Test

class NoAutoincrementWithoutNeedRuleTest {
    @Test
    fun `reports autoincrement`() {
        NoAutoincrementWithoutNeedRule().assertOne("CREATE TABLE player (id INTEGER PRIMARY KEY AUTOINCREMENT);")
    }
}
