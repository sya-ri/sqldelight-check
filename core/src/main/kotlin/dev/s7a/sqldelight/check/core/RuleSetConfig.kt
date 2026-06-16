package dev.s7a.sqldelight.check.core

import dev.s7a.sqldelight.check.api.RuleSetId

/**
 * Rule set configuration before database-specific overrides are applied.
 */
public class RuleSetConfig(
    /**
     * Rule set ID.
     */
    public val id: RuleSetId,
    /**
     * Configured rule set enablement. `null` lets sqldelight-check decide automatically.
     */
    public val enablement: Boolean? = null,
) {
    override fun equals(other: Any?): Boolean =
        this === other ||
            other is RuleSetConfig &&
            id == other.id &&
            enablement == other.enablement

    override fun hashCode(): Int = 31 * id.hashCode() + enablement.hashCode()

    override fun toString(): String = "RuleSetConfig(id=$id, enablement=$enablement)"
}
