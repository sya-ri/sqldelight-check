package dev.s7a.sqldelight.check.rules.hsql

import dev.s7a.sqldelight.check.api.QualifiedRuleId



import dev.s7a.sqldelight.check.api.DialectCapabilities
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.RuleSetId
import dev.s7a.sqldelight.check.rule.api.RuleSetProvider
import dev.s7a.sqldelight.check.rules.hsql.rules.NoDatabaseFileSettingsRule
import dev.s7a.sqldelight.check.rules.hsql.rules.NoSystemOperationsRule
import dev.s7a.sqldelight.check.rules.hsql.rules.NoTextTableSourceRule
import java.util.ServiceLoader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for the HSQL rule set.
 */
class HsqlRuleSetProviderTest {
    @Test
    fun `hsql rule set provides hsql rules`() {
        val provider = HsqlRuleSetProvider()
        val rules = provider.ruleProviders().map { ruleProvider -> ruleProvider.create() }

        assertEquals(RuleSetId("hsql"), provider.id)
        assertEquals(setOf(DialectCapabilities.Hsql), rules.map { rule -> rule.targetCapability }.toSet())
        assertEquals(
            setOf(
                qualifiedRuleId("hsql:no-database-file-settings"),
                qualifiedRuleId("hsql:no-system-operations"),
                qualifiedRuleId("hsql:no-text-table-source"),
            ),
            rules.map { rule -> QualifiedRuleId(provider.id, rule.id) }.toSet(),
        )
        assertTrue(provider.ruleProviders().any { ruleProvider -> ruleProvider.create() is NoDatabaseFileSettingsRule })
        assertTrue(provider.ruleProviders().any { ruleProvider -> ruleProvider.create() is NoSystemOperationsRule })
        assertTrue(provider.ruleProviders().any { ruleProvider -> ruleProvider.create() is NoTextTableSourceRule })
    }

    @Test
    fun `hsql rule set is visible to service loader`() {
        val providers = ServiceLoader.load(RuleSetProvider::class.java).map { provider -> provider.id }.toSet()

        assertTrue(RuleSetId("hsql") in providers)
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
