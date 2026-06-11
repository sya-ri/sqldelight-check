package dev.s7a.sqldelight.check.reporter.api

import dev.s7a.sqldelight.check.api.Diagnostic

/**
 * Immutable report data passed to reporters.
 */
public class Report(
    /**
     * Diagnostics collected during a task execution.
     */
    public val diagnostics: List<Diagnostic>,
) {
    override fun equals(other: Any?): Boolean =
        this === other ||
            other is Report &&
            diagnostics == other.diagnostics

    override fun hashCode(): Int = diagnostics.hashCode()

    override fun toString(): String = "Report(diagnostics=$diagnostics)"
}
