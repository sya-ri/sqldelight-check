package dev.s7a.sqldelight.check.rules.hsql

import dev.s7a.sqldelight.check.api.DialectCapabilities
import dev.s7a.sqldelight.check.api.RuleSetId
import dev.s7a.sqldelight.check.rule.api.RuleSetProvider
import java.util.ServiceLoader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for the HSQL rule set.
 */
class HsqlRuleSetProviderTest {
    @Test
    fun `hsql rule set starts empty`() {
        val provider = HsqlRuleSetProvider()

        assertEquals(RuleSetId("hsql"), provider.id)
        assertEquals(DialectCapabilities.Hsql, provider.targetCapability)
        assertEquals(emptySet(), provider.ruleProviders())
    }

    @Test
    fun `hsql rule set is visible to service loader`() {
        val providers = ServiceLoader.load(RuleSetProvider::class.java).map { provider -> provider.id }.toSet()

        assertTrue(RuleSetId("hsql") in providers)
    }
}
