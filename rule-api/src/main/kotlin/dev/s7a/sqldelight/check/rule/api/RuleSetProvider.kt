package dev.s7a.sqldelight.check.rule.api

import dev.s7a.sqldelight.check.api.RuleSetId

/**
 * Provides a named collection of rules.
 */
public interface RuleSetProvider {
    /**
     * Rule set ID advertised to Gradle DSL and reports.
     */
    public val id: RuleSetId

    /**
     * Providers for rules in this set.
     */
    public fun ruleProviders(): Set<RuleProvider>
}
