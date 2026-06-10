package dev.s7a.sqldelight.check.core

import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.RuleSetId

/**
 * Rule set configuration before database-specific overrides are applied.
 */
public data class RuleSetConfig(
    /** Rule set ID. */
    public val id: RuleSetId,
    /** Configured rule set enablement. */
    public val enablement: Enablement,
)
