package dev.s7a.sqldelight.check.reporter.html

import dev.s7a.sqldelight.check.reporter.api.Report
import dev.s7a.sqldelight.check.reporter.api.ReportOutput
import dev.s7a.sqldelight.check.reporter.api.Reporter

/**
 * Reporter that writes sqldelight-check diagnostics as HTML.
 */
internal object HtmlReporter : Reporter {
    override fun write(
        report: Report,
        output: ReportOutput,
    ) {
        output.file().use { file ->
            file.write(report.toHtml().toByteArray())
        }
    }
}
