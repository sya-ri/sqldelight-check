package dev.s7a.sqldelight.check.rules.postgres

import dev.s7a.sqldelight.check.api.DialectCapabilities
import dev.s7a.sqldelight.check.api.DialectCapability
import dev.s7a.sqldelight.check.api.RuleSetId
import dev.s7a.sqldelight.check.rule.api.RuleProvider
import dev.s7a.sqldelight.check.rule.api.RuleSetProvider
import dev.s7a.sqldelight.check.rules.postgres.rules.ExcessiveLocksRule
import dev.s7a.sqldelight.check.rules.postgres.rules.NoAddColumnWithVolatileDefaultRule
import dev.s7a.sqldelight.check.rules.postgres.rules.NoConcurrentIndexInTransactionRule
import dev.s7a.sqldelight.check.rules.postgres.rules.NoDropColumnRule
import dev.s7a.sqldelight.check.rules.postgres.rules.NoRenameColumnRule
import dev.s7a.sqldelight.check.rules.postgres.rules.NoRenameTableRule
import dev.s7a.sqldelight.check.rules.postgres.rules.NoSetNotNullOnExistingColumnRule
import dev.s7a.sqldelight.check.rules.postgres.rules.PreferIdentityOverSerialRule
import dev.s7a.sqldelight.check.rules.postgres.rules.ReindexConcurrentlyRule
import dev.s7a.sqldelight.check.rules.postgres.rules.RequireConcurrentIndexRule
import dev.s7a.sqldelight.check.rules.postgres.rules.RequireNotValidConstraintRule
import dev.s7a.sqldelight.check.rules.postgres.rules.RiskyAlterTableRule

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
            RuleProvider(::RequireConcurrentIndexRule),
            RuleProvider(::NoConcurrentIndexInTransactionRule),
            RuleProvider(::RequireNotValidConstraintRule),
            RuleProvider(::NoSetNotNullOnExistingColumnRule),
            RuleProvider(::NoAddColumnWithVolatileDefaultRule),
            RuleProvider(::PreferIdentityOverSerialRule),
            RuleProvider(::NoDropColumnRule),
            RuleProvider(::NoRenameColumnRule),
            RuleProvider(::NoRenameTableRule),
            RuleProvider(::ReindexConcurrentlyRule),
            RuleProvider(::RiskyAlterTableRule),
        )
}
