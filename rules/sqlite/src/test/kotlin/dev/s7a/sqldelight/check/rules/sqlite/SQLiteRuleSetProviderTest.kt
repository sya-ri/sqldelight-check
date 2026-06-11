package dev.s7a.sqldelight.check.rules.sqlite

import dev.s7a.sqldelight.check.api.DialectCapabilities
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.RuleSetId
import dev.s7a.sqldelight.check.rule.api.RuleSetProvider
import java.util.ServiceLoader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for the SQLite rule set.
 */
class SQLiteRuleSetProviderTest {
    @Test
    fun `sqlite rule set provides sqlite rules`() {
        val provider = SQLiteRuleSetProvider()
        val ruleIds = provider.ruleProviders().map { ruleProvider -> ruleProvider.create().id }.toSet()

        assertEquals(RuleSetId("sqlite"), provider.id)
        assertEquals(DialectCapabilities.SQLite, provider.targetCapability)
        assertEquals(setOf(RuleId("sqlite:foreign-keys-restored")), ruleIds)
    }

    @Test
    fun `sqlite rule set is visible to service loader`() {
        val providers = ServiceLoader.load(RuleSetProvider::class.java).map { provider -> provider.id }.toSet()

        assertTrue(RuleSetId("sqlite") in providers)
    }
}
