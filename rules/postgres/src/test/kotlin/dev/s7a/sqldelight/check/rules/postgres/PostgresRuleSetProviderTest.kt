package dev.s7a.sqldelight.check.rules.postgres

import dev.s7a.sqldelight.check.api.DialectCapabilities
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.RuleSetId
import dev.s7a.sqldelight.check.rule.api.RuleSetProvider
import java.util.ServiceLoader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for the PostgreSQL rule set.
 */
class PostgresRuleSetProviderTest {
    @Test
    fun `postgres rule set provides postgres rules`() {
        val provider = PostgresRuleSetProvider()
        val rules = provider.ruleProviders().map { ruleProvider -> ruleProvider.create() }
        val ruleIds = rules.map { rule -> rule.id }.toSet()

        assertEquals(RuleSetId("postgres"), provider.id)
        assertEquals(setOf(DialectCapabilities.PostgreSql), rules.map { rule -> rule.targetCapability }.toSet())
        assertEquals(
            setOf(
                RuleId("postgres:excessive-locks"),
                RuleId("postgres:require-concurrent-index"),
                RuleId("postgres:no-concurrent-index-in-transaction"),
                RuleId("postgres:require-not-valid-constraint"),
                RuleId("postgres:no-set-not-null-on-existing-column"),
                RuleId("postgres:no-add-column-with-volatile-default"),
                RuleId("postgres:prefer-identity-over-serial"),
                RuleId("postgres:no-drop-column"),
                RuleId("postgres:no-rename-column"),
                RuleId("postgres:no-rename-table"),
                RuleId("postgres:reindex-concurrently"),
                RuleId("postgres:risky-alter-table"),
            ),
            ruleIds,
        )
    }

    @Test
    fun `postgres rule set is visible to service loader`() {
        val providers = ServiceLoader.load(RuleSetProvider::class.java).map { provider -> provider.id }.toSet()

        assertTrue(RuleSetId("postgres") in providers)
    }
}
