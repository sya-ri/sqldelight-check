package dev.s7a.sqldelight.check.gradle

import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.DatabaseContext
import dev.s7a.sqldelight.check.api.LogLevel
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SourceFile
import dev.s7a.sqldelight.check.core.AnalysisTrace
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
     * Log output detail for this task execution.
     */
    @get:Input
    public abstract val logLevel: Property<LogLevel>

    /**
     * Runs SQLDelight project detection, core analysis, and report writing.
     */
    @TaskAction
    public fun run() {
        val extension = project.extensions.getByType(SqlDelightCheckExtension::class.java)
        val logLevel = logLevel.get()
        val config = extension.toCheckConfig(logLevel)
        val traceCollector = RuleTraceCollector()
        val trace = tracing(logLevel, traceCollector)
        var result = analyze(config, trace)
        if (applyFixes.get()) {
            val changedFiles = applyDiagnosticFixes(result.diagnostics, config.allowUnsafeWrites)
            if (changedFiles > 0) {
                logger.lifecycle("Applied sqldelight-check fixes to {} file(s).", changedFiles)
                result = analyze(config, trace)
            }
        }

        writeReports(extension, result.diagnostics)
        logRuleHits(logLevel, traceCollector.traces, result.diagnostics)
        logger.lifecycle("sqldelight-check analyzed {} SQLDelight database(s).", result.databaseCount)

        val errorCount = result.diagnostics.count { diagnostic -> diagnostic.severity == Severity.Error }
        if (errorCount > 0) {
            throw GradleException("sqldelight-check found $errorCount error diagnostic(s).")
        }
    }

    private fun analyze(
        config: CheckConfig,
        trace: AnalysisTrace,
    ): AnalysisRunResult {
        val inputs = SqlDelightProjectResolver(project).resolve()
        val diagnostics =
            SqlDelightCheckEngine().run(
                inputs = inputs.map { input -> input.analysisInput },
                ruleSetProviders = project.sqldelightCheckRuleRegistry().providers(),
                config = config,
                trace = trace,
            )
        return AnalysisRunResult(databaseCount = inputs.size, diagnostics = diagnostics)
    }

    private fun tracing(
        logLevel: LogLevel,
        traceCollector: RuleTraceCollector,
    ): AnalysisTrace =
        object : AnalysisTrace {
            override fun databaseFiles(
                database: DatabaseContext,
                files: List<SourceFile>,
            ) {
                if (!logLevel.logsFiles) return
                logger.lifecycle("sqldelight-check [{}] files ({}):", database.name, files.size)
                files.forEach { file ->
                    logger.lifecycle("sqldelight-check [{}]   - {}", database.name, file.path)
                }
            }

            override fun fileRules(
                database: DatabaseContext,
                file: SourceFile,
                ruleIds: List<RuleId>,
            ) {
                if (!logLevel.logsRules) return
                traceCollector.record(database.name, file.path, ruleIds)
            }
        }

    private fun logRuleHits(
        logLevel: LogLevel,
        traces: List<FileRuleTrace>,
        diagnostics: List<Diagnostic>,
    ) {
        if (!logLevel.logsRules) return
        val hitRuleIdsByFile =
            diagnostics
                .filter { diagnostic -> diagnostic.file != null && diagnostic.ruleId != null }
                .groupBy { diagnostic -> diagnostic.file!!.path }
                .mapValues { (_, fileDiagnostics) ->
                    fileDiagnostics.mapTo(linkedSetOf()) { diagnostic -> diagnostic.ruleId!!.value }
                }

        traces.forEach { trace ->
            logger.lifecycle("sqldelight-check [{}] {} rules ({}):", trace.databaseName, trace.filePath, trace.ruleIds.size)
            if (trace.ruleIds.isEmpty()) {
                logger.lifecycle("sqldelight-check [{}]   - (none)", trace.databaseName)
                return@forEach
            }

            val hitRuleIds = hitRuleIdsByFile[trace.filePath].orEmpty()
            trace.ruleIds.forEach { ruleId ->
                val marker = if (ruleId.value in hitRuleIds) "x" else " "
                logger.lifecycle("sqldelight-check [{}] - [{}] {}", trace.databaseName, marker, ruleId.value)
            }
        }
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

private data class RuleTraceCollector(
    val traces: MutableList<FileRuleTrace> = mutableListOf(),
) {
    /**
     * Records the rule IDs that were considered for one file.
     */
    public fun record(
        databaseName: String,
        filePath: String,
        ruleIds: List<RuleId>,
    ) {
        traces += FileRuleTrace(databaseName = databaseName, filePath = filePath, ruleIds = ruleIds)
    }
}

private data class FileRuleTrace(
    val databaseName: String,
    val filePath: String,
    val ruleIds: List<RuleId>,
)
