package dev.s7a.sqldelight.check.rules.mysql.rules

import kotlin.test.Test
import kotlin.test.assertEquals

class NoUtf8CharsetRuleTest {
    @Test
    fun `reports utf8 charset`() {
        NoUtf8CharsetRule().assertOne("CREATE TABLE player (name TEXT) CHARSET=utf8;")
        assertEquals(emptyList(), NoUtf8CharsetRule().diagnostics("CREATE TABLE player (name TEXT) CHARSET=utf8mb4;"))
    }
}
