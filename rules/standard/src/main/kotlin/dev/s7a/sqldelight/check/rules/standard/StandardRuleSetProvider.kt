package dev.s7a.sqldelight.check.rules.standard

import dev.s7a.sqldelight.check.api.RuleSetId
import dev.s7a.sqldelight.check.rules.standard.rules.FinalNewlineRule
import dev.s7a.sqldelight.check.rules.standard.rules.NoTrailingWhitespaceRule
import dev.s7a.sqldelight.check.rule.api.RuleProvider
import dev.s7a.sqldelight.check.rule.api.RuleSetProvider

/**
 * Built-in common rule set for dialect-independent SQL and SQLDelight rules.
 */
public class StandardRuleSetProvider : RuleSetProvider {
    override val id: RuleSetId = RuleSetId("standard")

    /**
     * Returns rule providers in the standard rule set.
     *
     */
    override fun ruleProviders(): Set<RuleProvider> =
        setOf(
            RuleProvider(::NoTrailingWhitespaceRule),
            RuleProvider(::FinalNewlineRule),
        )
}
