package dev.s7a.sqldelight.check.gradle

import dev.s7a.sqldelight.check.core.SqlDelightCheckEngine
import dev.s7a.sqldelight.check.reporter.api.Report
import org.gradle.api.GradleException
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * Base task for sqldelight-check operations.
 */
public abstract class SqlDelightCheckTask : DefaultTask() {
    /**
     * Runs the placeholder task implementation.
     *
     * FIXME: Connect tasks to SQLDelight project detection and write handling.
     */
    @TaskAction
    public fun run() {
        val extension = project.extensions.getByType(SqlDelightCheckExtension::class.java)
        val diagnostics = SqlDelightCheckEngine().run()
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
    }
}
