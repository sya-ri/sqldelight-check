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
        val ruleIds = provider.ruleProviders().map { ruleProvider -> ruleProvider.create().id }.toSet()

        assertEquals(RuleSetId("postgres"), provider.id)
        assertEquals(DialectCapabilities.PostgreSql, provider.targetCapability)
        assertEquals(
            setOf(
                RuleId("postgres:excessive-locks"),
                RuleId("postgres:reindex-concurrently"),
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
