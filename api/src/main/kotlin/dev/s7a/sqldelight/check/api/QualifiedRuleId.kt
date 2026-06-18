package dev.s7a.sqldelight.check.api

/**
 * Identifies a rule after combining a rule set ID and a rule-local ID.
 */
public class QualifiedRuleId(
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

    override fun equals(other: Any?): Boolean =
        this === other ||
            other is QualifiedRuleId &&
            ruleSetId == other.ruleSetId &&
            ruleId == other.ruleId

    override fun hashCode(): Int {
        var result = ruleSetId.hashCode()
        result = 31 * result + ruleId.hashCode()
        return result
    }

    public companion object {
        /**
         * Creates a qualified rule ID from a `rule-set:rule-name` value.
         */
        public operator fun invoke(value: String): QualifiedRuleId {
            val delimiter = value.indexOf(':')
            require(delimiter > 0 && delimiter < value.lastIndex) {
                "Qualified rule ID must use the rule-set:rule-name form."
            }
            return QualifiedRuleId(
                ruleSetId = RuleSetId(value.substring(0, delimiter)),
                ruleId = RuleId(value.substring(delimiter + 1)),
            )
        }
    }
}
