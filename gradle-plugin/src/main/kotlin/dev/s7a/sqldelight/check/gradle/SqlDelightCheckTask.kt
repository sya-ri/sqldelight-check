package dev.s7a.sqldelight.check.gradle

import dev.s7a.sqldelight.check.core.SqlDelightCheckEngine
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.reporter.api.Report
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

/**
 * Base task for sqldelight-check operations.
 */
public abstract class SqlDelightCheckTask : DefaultTask() {
    /**
     * Runs SQLDelight project detection, adapter analysis, and report writing.
     */
    @TaskAction
    public fun run() {
        val extension = project.extensions.getByType(SqlDelightCheckExtension::class.java)
        val inputs = SqlDelightProjectResolver(project).resolve()
        val diagnostics =
            inputs.flatMap { input ->
                val provider =
                    project
                        .sqldelightCheckAdapterRegistry()
                        .find(input.sqlDelightVersion)
                        ?: throw GradleException(
                            "No sqldelight-check adapter found for SQLDelight ${input.sqlDelightVersion}.",
                        )
                SqlDelightCheckEngine().run(
                    inputs = listOf(input.analysisInput),
                    adapter = provider.create(),
                )
            }
        val report = Report(diagnostics)
        val registry = project.sqldelightCheckReporterRegistry()

        extension.reports
            .filter { reporter -> reporter.required.get() }
            .forEach { reporter ->
                val provider =
                    registry.find(reporter.name)
                        ?: throw GradleException("sqldelight-check reporter '${reporter.name}' was not found on the runtime classpath.")
                val outputFile = reporter.outputFile.get().asFile
                outputFile.parentFile.mkdirs()
                outputFile.outputStream().use { output ->
                    provider.create().write(report, output)
                }
                logger.lifecycle("Wrote sqldelight-check {} report to {}", reporter.name, outputFile)
            }
        logger.lifecycle("sqldelight-check analyzed {} SQLDelight database(s).", inputs.size)

        val errorCount = diagnostics.count { diagnostic -> diagnostic.severity == Severity.Error }
        if (errorCount > 0) {
            throw GradleException("sqldelight-check found $errorCount error diagnostic(s).")
        }
    }
}
