package dev.s7a.sqldelight.check.core

import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.QualifiedRuleId
import dev.s7a.sqldelight.check.api.Severity

/**
 * Resolved configuration for one rule after global and database-specific overrides are applied.
 */
public class ResolvedRuleConfig(
    /**
     * Rule ID being configured.
     */
    public val ruleId: QualifiedRuleId,
    /**
     * Final enablement before `Auto` applicability is evaluated.
     */
    public val enablement: Enablement,
    /**
     * Final severity assigned to diagnostics from this rule.
     */
    public val severity: Severity,
    /**
     * Final rule-specific string options after global and database-specific values are merged.
     */
    public val options: Map<String, String>,
) {
    override fun equals(other: Any?): Boolean =
        this === other ||
            other is ResolvedRuleConfig &&
            ruleId == other.ruleId &&
            enablement == other.enablement &&
            severity == other.severity &&
            options == other.options

    override fun hashCode(): Int {
        var result = ruleId.hashCode()
        result = 31 * result + enablement.hashCode()
        result = 31 * result + severity.hashCode()
        result = 31 * result + options.hashCode()
        return result
    }

    override fun toString(): String =
        "ResolvedRuleConfig(ruleId=$ruleId, enablement=$enablement, severity=$severity, options=$options)"
}
