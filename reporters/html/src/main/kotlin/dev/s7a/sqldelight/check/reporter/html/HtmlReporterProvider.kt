package dev.s7a.sqldelight.check.reporter.html

import dev.s7a.sqldelight.check.reporter.api.Report
import dev.s7a.sqldelight.check.reporter.api.Reporter
import dev.s7a.sqldelight.check.reporter.api.ReporterProvider
import java.io.OutputStream

/**
 * Provider for the built-in HTML reporter.
 */
public class HtmlReporterProvider : ReporterProvider {
    override val id: String = "html"

    override fun create(options: Map<String, String>): Reporter = HtmlReporter
}

private object HtmlReporter : Reporter {
    override fun write(
        report: Report,
        output: OutputStream,
    ) {
        // FIXME: Replace with a useful navigable HTML report.
        output.write("<!doctype html><title>sqldelight-check</title><p>${report.diagnostics.size} diagnostics</p>".toByteArray())
    }
}

