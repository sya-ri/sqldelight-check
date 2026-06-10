package dev.s7a.sqldelight.check.reporter.api

import dev.s7a.sqldelight.check.api.Diagnostic
import java.io.OutputStream

/**
 * Immutable report data passed to reporters.
 */
public data class Report(
    /** Diagnostics collected during a task execution. */
    public val diagnostics: List<Diagnostic>,
)

/**
 * Writes a report in one output format.
 */
public interface Reporter {
    /**
     * Writes this report to the supplied output stream.
     */
    public fun write(
        report: Report,
        output: OutputStream,
    )
}

/**
 * Provides reporter instances discovered from built-in modules or external dependencies.
 */
public interface ReporterProvider {
    /** Reporter ID used in the Gradle DSL. */
    public val id: String

    /**
     * Creates a reporter instance.
     */
    public fun create(options: Map<String, String> = emptyMap()): Reporter
}

