package dev.s7a.sqldelight.check.reporter.text

import dev.s7a.sqldelight.check.reporter.api.Report
import dev.s7a.sqldelight.check.reporter.api.Reporter
import dev.s7a.sqldelight.check.reporter.api.ReporterProvider
import java.io.OutputStream

/**
 * Provider for the built-in plain text reporter.
 */
public class TextReporterProvider : ReporterProvider {
    override val id: String = "text"

    override fun create(options: Map<String, String>): Reporter = TextReporter
}

private object TextReporter : Reporter {
    override fun write(
        report: Report,
        output: OutputStream,
    ) {
        output.write("sqldelight-check diagnostics: ${report.diagnostics.size}\n".toByteArray())
    }
}
