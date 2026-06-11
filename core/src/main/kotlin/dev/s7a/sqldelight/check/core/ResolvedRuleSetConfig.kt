package dev.s7a.sqldelight.check.core

import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.RuleSetId

/**
 * Resolved configuration for one rule set.
 */
internal class ResolvedRuleSetConfig(
    /**
     * Rule set ID being configured.
     */
    val ruleSetId: RuleSetId,
    /**
     * Final enablement before rule-level overrides are applied.
     */
    val enablement: Enablement,
) {
    override fun equals(other: Any?): Boolean =
        this === other ||
            other is ResolvedRuleSetConfig &&
            ruleSetId == other.ruleSetId &&
            enablement == other.enablement

    override fun hashCode(): Int = 31 * ruleSetId.hashCode() + enablement.hashCode()

    override fun toString(): String = "ResolvedRuleSetConfig(ruleSetId=$ruleSetId, enablement=$enablement)"
}
