package dev.s7a.sqldelight.check.rules.postgres

import dev.s7a.sqldelight.check.api.DialectCapabilities
import dev.s7a.sqldelight.check.api.DialectCapability
import dev.s7a.sqldelight.check.api.RuleSetId
import dev.s7a.sqldelight.check.rule.api.RuleProvider
import dev.s7a.sqldelight.check.rule.api.RuleSetProvider
import dev.s7a.sqldelight.check.rules.postgres.rules.ExcessiveLocksRule
import dev.s7a.sqldelight.check.rules.postgres.rules.ReindexConcurrentlyRule

/**
 * Provides rules for PostgreSQL dialect projects.
 */
public class PostgresRuleSetProvider : RuleSetProvider {
    override val id: RuleSetId = RuleSetId("postgres")

    /**
     * Dialect capability this rule set targets.
     *
     * FIXME: Move this to RuleSetProvider when the public API supports rule-set-level capability gates.
     */
    public val targetCapability: DialectCapability = DialectCapabilities.PostgreSql

    override fun ruleProviders(): Set<RuleProvider> =
        setOf(
            RuleProvider(::ExcessiveLocksRule),
            RuleProvider(::ReindexConcurrentlyRule),
        )
}
