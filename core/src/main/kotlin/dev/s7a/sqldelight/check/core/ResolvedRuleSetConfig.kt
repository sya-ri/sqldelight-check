package dev.s7a.sqldelight.check.core

import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.RuleSetId

/**
 * Resolved configuration for one rule set.
 */
public data class ResolvedRuleSetConfig(
    /**
     * Rule set ID being configured.
     */
    public val ruleSetId: RuleSetId,
    /**
     * Final enablement before rule-level overrides are applied.
     */
    public val enablement: Enablement,
)
