package dev.s7a.sqldelight.check.reporter.githubannotations

import dev.s7a.sqldelight.check.reporter.api.Reporter
import dev.s7a.sqldelight.check.reporter.api.ReporterId
import dev.s7a.sqldelight.check.reporter.api.ReporterProvider

/**
 * Provider for GitHub Actions workflow command annotations.
 */
public class GitHubAnnotationsReporterProvider : ReporterProvider {
    override val id: ReporterId = ReporterId("github-annotations")

    override fun create(options: Map<String, String>): Reporter = GitHubAnnotationsReporter
}
