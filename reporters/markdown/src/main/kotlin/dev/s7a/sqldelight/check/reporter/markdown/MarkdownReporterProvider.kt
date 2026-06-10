package dev.s7a.sqldelight.check.reporter.markdown

import dev.s7a.sqldelight.check.reporter.api.Report
import dev.s7a.sqldelight.check.reporter.api.Reporter
import dev.s7a.sqldelight.check.reporter.api.ReporterProvider
import java.io.OutputStream

/**
 * Provider for the built-in Markdown reporter.
 */
public class MarkdownReporterProvider : ReporterProvider {
    override val id: String = "markdown"

    override fun create(options: Map<String, String>): Reporter = MarkdownReporter
}

private object MarkdownReporter : Reporter {
    override fun write(
        report: Report,
        output: OutputStream,
    ) {
        // FIXME: Replace with a complete Markdown summary suitable for CI job summaries.
        output.write("# sqldelight-check\n\nDiagnostics: ${report.diagnostics.size}\n".toByteArray())
    }
}

