package dev.s7a.sqldelight.check.rules.mysql

import dev.s7a.sqldelight.check.api.RuleSetId
import dev.s7a.sqldelight.check.rule.api.RuleProvider
import dev.s7a.sqldelight.check.rule.api.RuleSetProvider
import dev.s7a.sqldelight.check.rules.mysql.rules.NoCopyAlgorithmRule
import dev.s7a.sqldelight.check.rules.mysql.rules.NoDisplayWidthIntegerRule
import dev.s7a.sqldelight.check.rules.mysql.rules.NoExclusiveLockRule
import dev.s7a.sqldelight.check.rules.mysql.rules.NoReplaceIntoRule
import dev.s7a.sqldelight.check.rules.mysql.rules.NoUtf8CharsetRule
import dev.s7a.sqldelight.check.rules.mysql.rules.NoZeroDateDefaultRule
import dev.s7a.sqldelight.check.rules.mysql.rules.RequireIndexPrefixLengthRule
import dev.s7a.sqldelight.check.rules.mysql.rules.RiskyAlterTableRule

/**
 * Provides rules for MySQL dialect projects.
 *
 * Individual rules declare the MySQL target capability so `Auto` enablement can
 * be resolved per rule.
 */
public class MySqlRuleSetProvider : RuleSetProvider {
    override val id: RuleSetId = RuleSetId("mysql")

    override fun ruleProviders(): Set<RuleProvider> =
        setOf(
            RuleProvider(::NoUtf8CharsetRule),
            RuleProvider(::NoCopyAlgorithmRule),
            RuleProvider(::NoExclusiveLockRule),
            RuleProvider(::NoReplaceIntoRule),
            RuleProvider(::NoZeroDateDefaultRule),
            RuleProvider(::NoDisplayWidthIntegerRule),
            RuleProvider(::RequireIndexPrefixLengthRule),
            RuleProvider(::RiskyAlterTableRule),
        )
}
