package dev.s7a.sqldelight.check.rules.mysql.rules

import kotlin.test.Test

class NoZeroDateDefaultRuleTest {
    @Test
    fun `reports zero date defaults`() {
        NoZeroDateDefaultRule().assertOne("CREATE TABLE event (created_at DATE DEFAULT '0000-00-00');")
    }
}
