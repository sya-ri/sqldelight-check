package dev.s7a.sqldelight.check.rules.hsql.rules

import kotlin.test.Test

class NoSystemOperationsRuleTest {
    @Test
    fun `reports system operations`() {
        NoSystemOperationsRule().assertOne("CHECKPOINT DEFRAG;")
        NoSystemOperationsRule().assertOne("BACKUP DATABASE TO 'backup.tar.gz' BLOCKING;")
        NoSystemOperationsRule().assertOne("PERFORM EXPORT SCRIPT FOR DATABASE STRUCTURE TO 'schema.sql';")
    }
}
