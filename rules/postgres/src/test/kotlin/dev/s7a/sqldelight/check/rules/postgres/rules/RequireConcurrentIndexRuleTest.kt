package dev.s7a.sqldelight.check.rules.postgres.rules

import dev.s7a.sqldelight.check.api.QualifiedRuleId


import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.RuleSetId
import kotlin.test.Test
import kotlin.test.assertEquals

class RequireConcurrentIndexRuleTest {
    @Test
    fun `reports non concurrent indexes`() {
        val diagnostics = RequireConcurrentIndexRule().diagnostics("CREATE INDEX player_name ON player(name);")

        assertEquals(1, diagnostics.size)
        assertEquals(qualifiedRuleId("postgres:require-concurrent-index"), diagnostics.single().ruleId)
    }

    @Test
    fun `accepts concurrent indexes and ignores comments`() {
        assertEquals(
            emptyList(),
            RequireConcurrentIndexRule().diagnostics(
                """
                -- CREATE INDEX player_name ON player(name);
                CREATE INDEX CONCURRENTLY player_name ON player(name);
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
