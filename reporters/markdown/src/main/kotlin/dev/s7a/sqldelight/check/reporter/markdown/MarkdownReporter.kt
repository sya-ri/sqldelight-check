package dev.s7a.sqldelight.check.reporter.markdown

import dev.s7a.sqldelight.check.reporter.api.Report
import dev.s7a.sqldelight.check.reporter.api.ReportOutput
import dev.s7a.sqldelight.check.reporter.api.Reporter

/**
 * Reporter that writes diagnostics as Markdown.
 */
internal object MarkdownReporter : Reporter {
    override fun write(
        report: Report,
        output: ReportOutput,
    ) {
        output.file().use { file ->
            file.write(report.toMarkdown().toByteArray())
        }
    }
}
