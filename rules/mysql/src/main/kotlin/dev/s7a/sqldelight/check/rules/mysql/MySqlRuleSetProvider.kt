package dev.s7a.sqldelight.check.rules.mysql

import dev.s7a.sqldelight.check.api.DialectCapabilities
import dev.s7a.sqldelight.check.api.DialectCapability
import dev.s7a.sqldelight.check.api.RuleSetId
import dev.s7a.sqldelight.check.rule.api.RuleProvider
import dev.s7a.sqldelight.check.rule.api.RuleSetProvider
import dev.s7a.sqldelight.check.rules.mysql.rules.NoReplaceIntoRule

/**
 * Provides rules for MySQL dialect projects.
 */
public class MySqlRuleSetProvider : RuleSetProvider {
    override val id: RuleSetId = RuleSetId("mysql")

    /**
     * Dialect capability this rule set targets.
     *
     * FIXME: Move this to RuleSetProvider when the public API supports rule-set-level capability gates.
     */
    public val targetCapability: DialectCapability = DialectCapabilities.MySql

    override fun ruleProviders(): Set<RuleProvider> =
        setOf(
            RuleProvider(::NoReplaceIntoRule),
        )
}
