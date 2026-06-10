package dev.s7a.sqldelight.check.core

import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity

/**
 * Rule configuration before database-specific overrides are applied.
 */
public data class RuleConfig(
    /** Rule ID. */
    public val id: RuleId,
    /** Configured rule enablement. */
    public val enablement: Enablement,
    /** Configured rule severity. */
    public val severity: Severity,
)
