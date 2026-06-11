package dev.s7a.sqldelight.check.rules.hsql

import dev.s7a.sqldelight.check.api.DialectCapabilities
import dev.s7a.sqldelight.check.api.DialectCapability
import dev.s7a.sqldelight.check.api.RuleSetId
import dev.s7a.sqldelight.check.rule.api.RuleProvider
import dev.s7a.sqldelight.check.rule.api.RuleSetProvider

/**
 * Provides rules for HSQL dialect projects.
 */
public class HsqlRuleSetProvider : RuleSetProvider {
    override val id: RuleSetId = RuleSetId("hsql")

    /**
     * Dialect capability this rule set targets.
     *
     * FIXME: Move this to RuleSetProvider when the public API supports rule-set-level capability gates.
     */
    public val targetCapability: DialectCapability = DialectCapabilities.Hsql

    override fun ruleProviders(): Set<RuleProvider> = emptySet()
}
