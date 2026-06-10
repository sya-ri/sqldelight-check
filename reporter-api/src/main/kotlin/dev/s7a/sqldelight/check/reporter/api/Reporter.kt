package dev.s7a.sqldelight.check.reporter.api

import java.io.OutputStream

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
