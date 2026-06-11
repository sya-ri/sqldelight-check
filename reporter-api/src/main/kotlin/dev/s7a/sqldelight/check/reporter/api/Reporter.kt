package dev.s7a.sqldelight.check.reporter.api

/**
 * Writes a report in one output format.
 */
public interface Reporter {
    /**
     * Writes this report to the supplied output target.
     */
    public fun write(
        report: Report,
        output: ReportOutput,
    )
}
