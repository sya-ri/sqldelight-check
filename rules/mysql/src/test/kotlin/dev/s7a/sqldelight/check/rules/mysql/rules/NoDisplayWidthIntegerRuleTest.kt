package dev.s7a.sqldelight.check.rules.mysql.rules

import kotlin.test.Test

class NoDisplayWidthIntegerRuleTest {
    @Test
    fun `reports display width integers`() {
        NoDisplayWidthIntegerRule().assertOne("CREATE TABLE player (score INT(11));")
    }
}
