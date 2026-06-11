package dev.s7a.sqldelight.check.rules.postgres

import dev.s7a.sqldelight.check.api.QualifiedRuleId



import dev.s7a.sqldelight.check.api.DialectCapability
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
        val ruleIds = rules.map { rule -> QualifiedRuleId(provider.id, rule.id) }.toSet()

        assertEquals(RuleSetId("postgres"), provider.id)
        assertEquals(setOf(DialectCapability.PostgreSql), rules.map { rule -> rule.targetCapability }.toSet())
        assertEquals(
            setOf(
                qualifiedRuleId("postgres:excessive-locks"),
                qualifiedRuleId("postgres:require-concurrent-index"),
                qualifiedRuleId("postgres:no-concurrent-index-in-transaction"),
                qualifiedRuleId("postgres:require-not-valid-constraint"),
                qualifiedRuleId("postgres:no-set-not-null-on-existing-column"),
                qualifiedRuleId("postgres:no-add-column-with-volatile-default"),
                qualifiedRuleId("postgres:prefer-identity-over-serial"),
                qualifiedRuleId("postgres:no-drop-column"),
                qualifiedRuleId("postgres:no-rename-column"),
                qualifiedRuleId("postgres:no-rename-table"),
                qualifiedRuleId("postgres:reindex-concurrently"),
                qualifiedRuleId("postgres:risky-alter-table"),
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

private fun qualifiedRuleId(value: String): QualifiedRuleId {
    val delimiter = value.indexOf(':')
    require(delimiter > 0 && delimiter < value.lastIndex)
    return QualifiedRuleId(
        ruleSetId = RuleSetId(value.substring(0, delimiter)),
        ruleId = RuleId(value.substring(delimiter + 1)),
    )
}
