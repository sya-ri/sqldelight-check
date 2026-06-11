package dev.s7a.sqldelight.check.rules.hsql.rules

import dev.s7a.sqldelight.check.api.Severity
import kotlin.test.Test
import kotlin.test.assertEquals

class NoSystemOperationsRuleTest {
    @Test
    fun `reports system operations`() {
        val diagnostics = NoSystemOperationsRule().diagnostics("CHECKPOINT DEFRAG;")

        assertEquals(1, diagnostics.size)
        assertEquals(Severity.Error, diagnostics.single().severity)
        NoSystemOperationsRule().assertOne("BACKUP DATABASE TO 'backup.tar.gz' BLOCKING;")
        NoSystemOperationsRule().assertOne("PERFORM EXPORT SCRIPT FOR DATABASE STRUCTURE TO 'schema.sql';")
    }
}
