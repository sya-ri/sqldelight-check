package dev.s7a.sqldelight.check.rules.mysql

import dev.s7a.sqldelight.check.api.DialectCapabilities
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
    fun `mysql rule set starts empty`() {
        val provider = MySqlRuleSetProvider()

        assertEquals(RuleSetId("mysql"), provider.id)
        assertEquals(DialectCapabilities.MySql, provider.targetCapability)
        assertEquals(emptySet(), provider.ruleProviders())
    }

    @Test
    fun `mysql rule set is visible to service loader`() {
        val providers = ServiceLoader.load(RuleSetProvider::class.java).map { provider -> provider.id }.toSet()

        assertTrue(RuleSetId("mysql") in providers)
    }
}
