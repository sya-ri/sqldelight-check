package dev.s7a.sqldelight.check.rule.api

/**
 * Creates fresh diagnostic refinement instances for thread-safe analysis.
 */
public fun interface DiagnosticRefinementProvider {
    /**
     * Creates a new diagnostic refinement instance.
     */
    public fun create(): DiagnosticRefinement
}
