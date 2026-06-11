package dev.s7a.sqldelight.check.core

import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity

/**
 * Rule configuration before database-specific overrides are applied.
 */
public class RuleConfig(
    /**
     * Rule ID.
     */
    public val id: RuleId,
    /**
     * Configured rule enablement.
     */
    public val enablement: Enablement,
    /**
     * Configured rule severity.
     */
    public val severity: Severity,
    /**
     * Rule-specific string options.
     */
    public val options: Map<String, String> = emptyMap(),
) {
    override fun equals(other: Any?): Boolean =
        this === other ||
            other is RuleConfig &&
            id == other.id &&
            enablement == other.enablement &&
            severity == other.severity &&
            options == other.options

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + enablement.hashCode()
        result = 31 * result + severity.hashCode()
        result = 31 * result + options.hashCode()
        return result
    }

    override fun toString(): String = "RuleConfig(id=$id, enablement=$enablement, severity=$severity, options=$options)"
}
