package dev.s7a.sqldelight.check.rules.mysql

import dev.s7a.sqldelight.check.api.QualifiedRuleId



import dev.s7a.sqldelight.check.api.DialectCapabilities
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.RuleSetId
import dev.s7a.sqldelight.check.rule.api.RuleSetProvider
import java.util.ServiceLoader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for the MySQL rule set.
 */
class MySqlRuleSetProviderTest {
    @Test
    fun `mysql rule set provides mysql rules`() {
        val provider = MySqlRuleSetProvider()
        val rules = provider.ruleProviders().map { ruleProvider -> ruleProvider.create() }
        val ruleIds = rules.map { rule -> QualifiedRuleId(provider.id, rule.id) }.toSet()

        assertEquals(RuleSetId("mysql"), provider.id)
        assertEquals(setOf(DialectCapabilities.MySql), rules.map { rule -> rule.targetCapability }.toSet())
        assertEquals(
            setOf(
                qualifiedRuleId("mysql:no-utf8-charset"),
                qualifiedRuleId("mysql:no-copy-algorithm"),
                qualifiedRuleId("mysql:no-exclusive-lock"),
                qualifiedRuleId("mysql:no-replace-into"),
                qualifiedRuleId("mysql:no-zero-date-default"),
                qualifiedRuleId("mysql:no-display-width-integer"),
                qualifiedRuleId("mysql:require-index-prefix-length"),
                qualifiedRuleId("mysql:risky-alter-table"),
            ),
            ruleIds,
        )
    }

    @Test
    fun `mysql rule set is visible to service loader`() {
        val providers = ServiceLoader.load(RuleSetProvider::class.java).map { provider -> provider.id }.toSet()

        assertTrue(RuleSetId("mysql") in providers)
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
