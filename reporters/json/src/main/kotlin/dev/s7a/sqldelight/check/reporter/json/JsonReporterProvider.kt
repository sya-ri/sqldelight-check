package dev.s7a.sqldelight.check.reporter.json

import dev.s7a.sqldelight.check.reporter.api.Report
import dev.s7a.sqldelight.check.reporter.api.Reporter
import dev.s7a.sqldelight.check.reporter.api.ReporterProvider
import java.io.OutputStream

/**
 * Provider for the built-in JSON reporter.
 */
public class JsonReporterProvider : ReporterProvider {
    override val id: String = "json"

    override fun create(options: Map<String, String>): Reporter = JsonReporter
}

private object JsonReporter : Reporter {
    override fun write(
        report: Report,
        output: OutputStream,
    ) {
        // FIXME: Replace this placeholder with the stable v0.1.0 JSON report schema.
        output.write("""{"diagnostics":${report.diagnostics.size}}""".toByteArray())
    }
}

