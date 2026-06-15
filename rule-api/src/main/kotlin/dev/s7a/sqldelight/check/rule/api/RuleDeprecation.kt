package dev.s7a.sqldelight.check.rule.api

import dev.s7a.sqldelight.check.api.QualifiedRuleId

/**
 * Deprecation metadata for a rule that remains available for explicit users.
 */
public class RuleDeprecation(
    /**
     * Human-readable migration note for this deprecated rule.
     */
    public val message: String,
    /**
     * Replacement rule, when a direct replacement exists.
     */
    public val replacement: QualifiedRuleId? = null,
) {
    override fun equals(other: Any?): Boolean =
        this === other ||
            other is RuleDeprecation &&
            message == other.message &&
            replacement == other.replacement

    override fun hashCode(): Int {
        var result = message.hashCode()
        result = 31 * result + (replacement?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String = "RuleDeprecation(message=$message, replacement=$replacement)"
}
