package dev.s7a.sqldelight.check.rules.mysql

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
        val ruleIds = rules.map { rule -> RuleId("${provider.id.value}:${rule.id}") }.toSet()

        assertEquals(RuleSetId("mysql"), provider.id)
        assertEquals(setOf(DialectCapabilities.MySql), rules.map { rule -> rule.targetCapability }.toSet())
        assertEquals(
            setOf(
                RuleId("mysql:no-utf8-charset"),
                RuleId("mysql:no-copy-algorithm"),
                RuleId("mysql:no-exclusive-lock"),
                RuleId("mysql:no-replace-into"),
                RuleId("mysql:no-zero-date-default"),
                RuleId("mysql:no-display-width-integer"),
                RuleId("mysql:require-index-prefix-length"),
                RuleId("mysql:risky-alter-table"),
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
