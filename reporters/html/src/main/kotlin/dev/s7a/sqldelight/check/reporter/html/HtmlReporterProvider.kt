package dev.s7a.sqldelight.check.reporter.html

import dev.s7a.sqldelight.check.reporter.api.Reporter
import dev.s7a.sqldelight.check.reporter.api.ReporterId
import dev.s7a.sqldelight.check.reporter.api.ReporterProvider

/**
 * Provider for the built-in HTML reporter.
 */
public class HtmlReporterProvider : ReporterProvider {
    override val id: ReporterId = ReporterId("html")

    override fun create(options: Map<String, String>): Reporter = HtmlReporter
}
