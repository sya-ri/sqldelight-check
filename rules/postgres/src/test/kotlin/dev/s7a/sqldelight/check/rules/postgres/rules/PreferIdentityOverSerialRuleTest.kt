package dev.s7a.sqldelight.check.rules.postgres.rules

import kotlin.test.Test

class PreferIdentityOverSerialRuleTest {
    @Test
    fun `reports serial types`() {
        PreferIdentityOverSerialRule().assertOne("CREATE TABLE player (id BIGSERIAL PRIMARY KEY);")
    }
}
