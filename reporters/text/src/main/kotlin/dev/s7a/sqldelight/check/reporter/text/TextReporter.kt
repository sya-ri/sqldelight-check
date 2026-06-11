package dev.s7a.sqldelight.check.reporter.text

import dev.s7a.sqldelight.check.reporter.api.Report
import dev.s7a.sqldelight.check.reporter.api.ReportOutput
import dev.s7a.sqldelight.check.reporter.api.Reporter

/**
 * Reporter that writes diagnostics as plain text.
 */
internal object TextReporter : Reporter {
    override fun write(
        report: Report,
        output: ReportOutput,
    ) {
        output.file().use { file ->
            file.write(report.render().toByteArray())
        }
    }
}
