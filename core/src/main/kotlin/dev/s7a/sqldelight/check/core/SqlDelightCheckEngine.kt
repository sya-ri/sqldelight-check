package dev.s7a.sqldelight.check.core

import dev.s7a.sqldelight.check.adapter.spi.AnalysisInput
import dev.s7a.sqldelight.check.adapter.spi.SqlDelightAdapter
import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.RuleSetId
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

/**
 * Resolved configuration for one rule set.
 */
public data class ResolvedRuleSetConfig(
    /** Rule set ID being configured. */
    public val ruleSetId: RuleSetId,
    /** Final enablement before rule-level overrides are applied. */
    public val enablement: Enablement,
)

/**
 * Entry point for sqldelight-check analysis.
 */
public class SqlDelightCheckEngine {
    /**
     * Runs analysis for all resolved SQLDelight databases.
     *
     * FIXME: Wire this to rule discovery, formatter execution, write validation, and reporter metadata.
     */
    public fun run(
        inputs: List<AnalysisInput> = emptyList(),
        adapter: SqlDelightAdapter? = null,
    ): List<Diagnostic> {
        if (adapter == null) return emptyList()
        return inputs.flatMap { input -> adapter.analyze(input).diagnostics }
    }
}
