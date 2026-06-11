package dev.s7a.sqldelight.check.rules.hsql.rules

import dev.s7a.sqldelight.check.api.QualifiedRuleId


import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.RuleSetId
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
        assertEquals(setOf(qualifiedRuleId("hsql:no-database-file-settings")), diagnostics.map { it.ruleId }.toSet())
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

private fun qualifiedRuleId(value: String): QualifiedRuleId {
    val delimiter = value.indexOf(':')
    require(delimiter > 0 && delimiter < value.lastIndex)
    return QualifiedRuleId(
        ruleSetId = RuleSetId(value.substring(0, delimiter)),
        ruleId = RuleId(value.substring(delimiter + 1)),
    )
}
