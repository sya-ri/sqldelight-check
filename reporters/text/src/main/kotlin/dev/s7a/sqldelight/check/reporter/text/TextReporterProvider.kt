package dev.s7a.sqldelight.check.reporter.text

import dev.s7a.sqldelight.check.reporter.api.Reporter
import dev.s7a.sqldelight.check.reporter.api.ReporterId
import dev.s7a.sqldelight.check.reporter.api.ReporterProvider

/**
 * Provider for the built-in plain text reporter.
 */
public class TextReporterProvider : ReporterProvider {
    override val id: ReporterId = ReporterId("text")

    override fun create(options: Map<String, String>): Reporter = TextReporter
}
