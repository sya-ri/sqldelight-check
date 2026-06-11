package dev.s7a.sqldelight.check.reporter.githubannotations

import dev.s7a.sqldelight.check.reporter.api.Report
import dev.s7a.sqldelight.check.reporter.api.ReportOutput
import dev.s7a.sqldelight.check.reporter.api.Reporter

/**
 * Reporter that writes GitHub Actions workflow command annotations.
 */
internal object GitHubAnnotationsReporter : Reporter {
    override fun write(
        report: Report,
        output: ReportOutput,
    ) {
        output.file().use { file ->
            file.write(report.toGitHubAnnotations().toByteArray())
        }
    }
}
