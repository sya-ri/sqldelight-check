package dev.s7a.sqldelight.check.rule.api

import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.QualifiedRuleId

/**
 * Refines diagnostics emitted by a rule.
 *
 * Rule sets can use refinements when a dialect or integration has enough
 * context to narrow diagnostics from another rule set without moving that
 * dialect-specific knowledge into the original rule.
 */
public interface DiagnosticRefinement {
    /**
     * Rule ID whose diagnostics this refinement should inspect.
     */
    public val targetRuleId: QualifiedRuleId

    /**
     * Returns the diagnostic to keep, or `null` to suppress it.
     */
    public fun refine(
        context: RuleContext,
        diagnostic: Diagnostic,
    ): Diagnostic?
}
