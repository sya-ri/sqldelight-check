package dev.s7a.sqldelight.check.gradle

import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.core.CheckConfig
import dev.s7a.sqldelight.check.core.FixApplier
import dev.s7a.sqldelight.check.core.SqlDelightCheckEngine
import dev.s7a.sqldelight.check.reporter.api.Report
import java.nio.charset.StandardCharsets
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

/**
 * Base task for sqldelight-check operations.
 */
public abstract class SqlDelightCheckTask : DefaultTask() {
    /**
     * Whether this task should apply allowed fixes to source files.
     */
    @get:Input
    public abstract val applyFixes: Property<Boolean>

    /**
     * Runs SQLDelight project detection, adapter analysis, and report writing.
     */
    @TaskAction
    public fun run() {
        val extension = project.extensions.getByType(SqlDelightCheckExtension::class.java)
        val config = extension.toCheckConfig()
        var result = analyze(config)
        if (applyFixes.get()) {
            val changedFiles = applyDiagnosticFixes(result.diagnostics, config.allowUnsafeWrites)
            if (changedFiles > 0) {
                logger.lifecycle("Applied sqldelight-check fixes to {} file(s).", changedFiles)
                result = analyze(config)
            }
        }

        writeReports(extension, result.diagnostics)
        logger.lifecycle("sqldelight-check analyzed {} SQLDelight database(s).", result.databaseCount)

        val errorCount = result.diagnostics.count { diagnostic -> diagnostic.severity == Severity.Error }
        if (errorCount > 0) {
            throw GradleException("sqldelight-check found $errorCount error diagnostic(s).")
        }
    }

    private fun analyze(config: CheckConfig): AnalysisRunResult {
        val inputs = SqlDelightProjectResolver(project).resolve()
        val diagnostics =
            SqlDelightCheckEngine().run(
                inputs = inputs.map { input -> input.analysisInput },
                ruleSetProviders = project.sqldelightCheckRuleRegistry().providers(),
                config = config,
            )
        return AnalysisRunResult(databaseCount = inputs.size, diagnostics = diagnostics)
    }

    private fun applyDiagnosticFixes(
        diagnostics: List<Diagnostic>,
        allowUnsafe: Boolean,
    ): Int {
        val applier = FixApplier()
        return diagnostics
            .filter { diagnostic -> diagnostic.file != null }
            .groupBy { diagnostic -> diagnostic.file?.path.orEmpty() }
            .count { (path, fileDiagnostics) ->
                val file = project.file(path)
                if (!file.isFile) return@count false

                val original = file.readText(StandardCharsets.UTF_8)
                val result = applier.apply(original, fileDiagnostics, allowUnsafe)
                if (result.content == original) return@count false

                file.writeText(result.content, StandardCharsets.UTF_8)
                true
            }
    }

    private fun writeReports(
        extension: SqlDelightCheckExtension,
        diagnostics: List<Diagnostic>,
    ) {
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
                    provider.create(reporter.options.get()).write(report, output)
                }
                logger.lifecycle("Wrote sqldelight-check {} report to {}", reporter.name, outputFile)
            }
    }
}

private data class AnalysisRunResult(
    val databaseCount: Int,
    val diagnostics: List<Diagnostic>,
)
