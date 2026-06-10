package dev.s7a.sqldelight.check.reporter.api

import dev.s7a.sqldelight.check.api.Diagnostic

/**
 * Immutable report data passed to reporters.
 */
public data class Report(
    /** Diagnostics collected during a task execution. */
    public val diagnostics: List<Diagnostic>,
)
