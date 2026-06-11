package dev.s7a.sqldelight.check.rules.hsql

import dev.s7a.sqldelight.check.api.RuleSetId
import dev.s7a.sqldelight.check.rule.api.RuleProvider
import dev.s7a.sqldelight.check.rule.api.RuleSetProvider
import dev.s7a.sqldelight.check.rules.hsql.rules.NoDatabaseFileSettingsRule
import dev.s7a.sqldelight.check.rules.hsql.rules.NoSystemOperationsRule
import dev.s7a.sqldelight.check.rules.hsql.rules.NoTextTableSourceRule

/**
 * Provides rules for HSQL dialect projects.
 *
 * The provider exposes a small rule batch focused on HSQL-specific migration
 * and schema safety checks.
 */
public class HsqlRuleSetProvider : RuleSetProvider {
    override val id: RuleSetId = RuleSetId("hsql")

    override fun ruleProviders(): Set<RuleProvider> =
        setOf(
            RuleProvider(::NoDatabaseFileSettingsRule),
            RuleProvider(::NoSystemOperationsRule),
            RuleProvider(::NoTextTableSourceRule),
        )
}
