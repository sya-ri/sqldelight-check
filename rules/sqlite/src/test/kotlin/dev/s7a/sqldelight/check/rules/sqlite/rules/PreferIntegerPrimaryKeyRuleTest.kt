package dev.s7a.sqldelight.check.rules.sqlite.rules

import kotlin.test.Test
import kotlin.test.assertEquals

class PreferIntegerPrimaryKeyRuleTest {
    @Test
    fun `reports non integer primary key rowid declarations`() {
        PreferIntegerPrimaryKeyRule().assertOne("CREATE TABLE player (id INT PRIMARY KEY);")
        assertEquals(emptyList(), PreferIntegerPrimaryKeyRule().diagnostics("CREATE TABLE player (id INTEGER PRIMARY KEY);"))
    }
}
