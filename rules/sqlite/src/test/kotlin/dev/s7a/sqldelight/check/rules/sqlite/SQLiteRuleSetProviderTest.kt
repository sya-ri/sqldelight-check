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
        val rules = provider.ruleProviders().map { ruleProvider -> ruleProvider.create() }
        val ruleIds = rules.map { rule -> RuleId("${provider.id.value}:${rule.id}") }.toSet()

        assertEquals(RuleSetId("sqlite"), provider.id)
        assertEquals(setOf(DialectCapabilities.SQLite), rules.map { rule -> rule.targetCapability }.toSet())
        assertEquals(
            setOf(
                RuleId("sqlite:consistent-conflict-resolution"),
                RuleId("sqlite:foreign-keys-restored"),
                RuleId("sqlite:prefer-integer-primary-key"),
                RuleId("sqlite:no-autoincrement-without-need"),
                RuleId("sqlite:no-alter-table-complex-change"),
                RuleId("sqlite:prefer-without-rowid-for-composite-pk"),
            ),
            ruleIds,
        )
    }

    @Test
    fun `sqlite rule set is visible to service loader`() {
        val providers = ServiceLoader.load(RuleSetProvider::class.java).map { provider -> provider.id }.toSet()

        assertTrue(RuleSetId("sqlite") in providers)
    }
}
