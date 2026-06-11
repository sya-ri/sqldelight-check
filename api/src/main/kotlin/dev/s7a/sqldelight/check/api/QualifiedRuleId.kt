package dev.s7a.sqldelight.check.api

/**
 * Identifies a rule after combining a rule set ID and a rule-local ID.
 */
public data class QualifiedRuleId(
    /**
     * Rule set that contributes the rule.
     */
    public val ruleSetId: RuleSetId,
    /**
     * Rule-local ID inside [ruleSetId].
     */
    public val ruleId: RuleId,
) {
    /**
     * User-facing ID value in the `rule-set:rule-name` form.
     */
    public val value: String = "${ruleSetId.value}:${ruleId.value}"

    override fun toString(): String = value
}
