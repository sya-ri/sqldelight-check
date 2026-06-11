package dev.s7a.sqldelight.check.rules.mysql.rules

import kotlin.test.Test

class NoCopyAlgorithmRuleTest {
    @Test
    fun `reports copy algorithm`() {
        NoCopyAlgorithmRule().assertOne("ALTER TABLE player ADD COLUMN score INT, ALGORITHM=COPY;")
    }
}
