package dev.s7a.sqldelight.check.reporter.markdown

import dev.s7a.sqldelight.check.reporter.api.Reporter
import dev.s7a.sqldelight.check.reporter.api.ReporterId
import dev.s7a.sqldelight.check.reporter.api.ReporterProvider

/**
 * Provider for the built-in Markdown reporter.
 */
public class MarkdownReporterProvider : ReporterProvider {
    override val id: ReporterId = ReporterId("markdown")

    override fun create(options: Map<String, String>): Reporter = MarkdownReporter
}
