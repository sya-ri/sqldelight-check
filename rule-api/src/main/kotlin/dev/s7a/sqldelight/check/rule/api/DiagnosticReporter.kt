package dev.s7a.sqldelight.check.rule.api

import dev.s7a.sqldelight.check.api.RuleDiagnostic

/**
 * Receives diagnostics emitted by a rule.
 */
public fun interface DiagnosticReporter {
    /**
     * Records a diagnostic.
     */
    public fun report(diagnostic: RuleDiagnostic)
}
