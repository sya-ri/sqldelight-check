package dev.s7a.sqldelight.check.rules.postgres.rules

import dev.s7a.sqldelight.check.api.Severity
import kotlin.test.Test
import kotlin.test.assertEquals

class NoDropColumnRuleTest {
    @Test
    fun `reports drop column`() {
        val diagnostics = NoDropColumnRule().diagnostics("ALTER TABLE player DROP COLUMN old_name;")

        assertEquals(1, diagnostics.size)
        assertEquals(Severity.Error, diagnostics.single().severity)
    }
}
