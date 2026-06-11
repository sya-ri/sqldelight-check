package dev.s7a.sqldelight.check.rules.hsql.rules

import dev.s7a.sqldelight.check.api.RuleId
import kotlin.test.Test
import kotlin.test.assertEquals

class NoDatabaseFileSettingsRuleTest {
    @Test
    fun `reports database and file settings`() {
        val diagnostics =
            NoDatabaseFileSettingsRule().diagnostics(
                """
                SET DATABASE DEFAULT TABLE TYPE CACHED;
                SET FILES WRITE DELAY 10;
                """,
            )

        assertEquals(2, diagnostics.size)
        assertEquals(setOf(RuleId("hsql:no-database-file-settings")), diagnostics.map { it.ruleId }.toSet())
    }

    @Test
    fun `ignores comments and quoted text`() {
        assertEquals(
            emptyList(),
            NoDatabaseFileSettingsRule().diagnostics(
                """
                -- SET DATABASE DEFAULT TABLE TYPE CACHED;
                INSERT INTO note VALUES ('SET FILES WRITE DELAY 10');
                """,
            ),
        )
    }
}
