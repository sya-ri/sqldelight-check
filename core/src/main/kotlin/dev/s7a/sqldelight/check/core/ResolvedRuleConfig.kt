package dev.s7a.sqldelight.check.core

import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity

/**
 * Resolved configuration for one rule after global and database-specific overrides are applied.
 */
public data class ResolvedRuleConfig(
    /** Rule ID being configured. */
    public val ruleId: RuleId,
    /** Final enablement before `Auto` applicability is evaluated. */
    public val enablement: Enablement,
    /** Final severity assigned to diagnostics from this rule. */
    public val severity: Severity,
)
