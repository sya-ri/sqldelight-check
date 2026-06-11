package dev.s7a.sqldelight.check.rules.mysql.rules

import dev.s7a.sqldelight.check.api.Severity
import kotlin.test.Test
import kotlin.test.assertEquals

class NoExclusiveLockRuleTest {
    @Test
    fun `reports exclusive lock`() {
        val diagnostics = NoExclusiveLockRule().diagnostics("ALTER TABLE player ADD COLUMN score INT, LOCK=EXCLUSIVE;")

        assertEquals(1, diagnostics.size)
        assertEquals(Severity.Error, diagnostics.single().severity)
    }
}
