package dev.s7a.sqldelight.check.gradle

import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.DatabaseContext
import dev.s7a.sqldelight.check.api.LogLevel
import dev.s7a.sqldelight.check.api.QualifiedRuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SourceFile
import dev.s7a.sqldelight.check.core.AnalysisTrace
import dev.s7a.sqldelight.check.core.CheckConfig
import dev.s7a.sqldelight.check.core.FixApplier
import dev.s7a.sqldelight.check.core.FixSkipReason
import dev.s7a.sqldelight.check.core.SqlDelightCheckEngine
import dev.s7a.sqldelight.check.reporter.api.Report
import dev.s7a.sqldelight.check.reporter.api.ReportOutput
import java.io.File
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Path
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
     * Runs SQLDelight project detection, rules, and report writing.
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
            val fixResult = applyDiagnosticFixes(result.diagnostics, config.allowUnsafeFixes)
            if (fixResult.changedFiles > 0) {
                logger.lifecycle("Applied sqldelight-check fixes to {} file(s).", fixResult.changedFiles)
                result = analyze(config, trace)
            }
            if (fixResult.skippedReasons.isNotEmpty()) {
                val skippedSummary =
                    fixResult.skippedReasons.entries.joinToString(", ") { (reason, count) ->
                        "${reason.logLabel()}=$count"
                    }
                logger.lifecycle("Skipped sqldelight-check fixes: {}.", skippedSummary)
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
        val inputs = SqlDelightProjectResolver(project, project.sqldelightCheckDialectsRegistry()).resolve()
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
                ruleIds: List<QualifiedRuleId>,
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
        val hitRuleIdsByFile = diagnosticRuleHitsByFile(diagnostics)

        traces.forEach { trace ->
            logger.lifecycle("sqldelight-check [{}] {} rules ({}):", trace.databaseName, trace.filePath, trace.ruleIds.size)
            if (trace.ruleIds.isEmpty()) {
                logger.lifecycle("sqldelight-check [{}]   - (none)", trace.databaseName)
                return@forEach
            }

            val hitRuleIds =
                hitRuleIdsByFile[
                    FileRuleKey(
                        databaseName = trace.databaseName,
                        filePath = trace.filePath,
                    ),
                ].orEmpty()
            trace.ruleIds.forEach { ruleId ->
                val marker = if (ruleId.value in hitRuleIds) "x" else " "
                logger.lifecycle("sqldelight-check [{}] - [{}] {}", trace.databaseName, marker, ruleId.value)
            }
        }
    }

    private fun applyDiagnosticFixes(
        diagnostics: List<Diagnostic>,
        allowUnsafe: Boolean,
    ): FixApplySummary {
        val applier = FixApplier()
        var changedFiles = 0
        val skippedReasons = linkedMapOf<FixSkipReason, Int>()
        diagnostics
            .filter { diagnostic -> diagnostic.file != null }
            .groupBy { diagnostic -> diagnostic.file?.path.orEmpty() }
            .forEach { (path, fileDiagnostics) ->
                val file = project.file(path)
                if (!file.isFile) return@forEach

                val original = file.readText(StandardCharsets.UTF_8)
                val result = applier.apply(original, fileDiagnostics, allowUnsafe)
                result.skippedFixDetails.forEach { skippedFix ->
                    skippedReasons[skippedFix.reason] = skippedReasons.getOrDefault(skippedFix.reason, 0) + 1
                }
                if (result.content == original) return@forEach

                file.writeText(result.content, StandardCharsets.UTF_8)
                changedFiles++
            }
        return FixApplySummary(changedFiles = changedFiles, skippedReasons = skippedReasons)
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
                val outputDirectory = reporter.outputDirectory.get().asFile
                provider
                    .create(reporter.resolvedOptions())
                    .write(
                        report,
                        GradleReportOutput(
                            primaryFile = outputFile,
                            outputDirectory = outputDirectory,
                        ),
                    )
                logger.lifecycle("Wrote sqldelight-check {} report to {}", reporter.name, outputFile)
            }
    }
}

private class GradleReportOutput(
    private val primaryFile: File,
    private val outputDirectory: File,
) : ReportOutput {
    override fun file(): OutputStream {
        primaryFile.parentFile.mkdirs()
        return primaryFile.outputStream()
    }

    override fun file(path: String): OutputStream {
        val relativePath = Path.of(path).normalize()
        require(!relativePath.isAbsolute && !relativePath.startsWith("..")) {
            "Report output path must be relative and stay inside the reporter output directory: $path"
        }

        val root = outputDirectory.toPath().toAbsolutePath().normalize()
        val file = root.resolve(relativePath).normalize()
        require(file.startsWith(root)) {
            "Report output path must stay inside the reporter output directory: $path"
        }

        val outputFile = file.toFile()
        outputFile.parentFile.mkdirs()
        return outputFile.outputStream()
    }
}

private data class AnalysisRunResult(
    val databaseCount: Int,
    val diagnostics: List<Diagnostic>,
)

private data class FixApplySummary(
    val changedFiles: Int,
    val skippedReasons: Map<FixSkipReason, Int>,
)

private fun FixSkipReason.logLabel(): String =
    when (this) {
        FixSkipReason.Unsafe -> "unsafe"
        FixSkipReason.InvalidRange -> "invalid-range"
        FixSkipReason.OverlappingEdits -> "overlapping-edits"
        FixSkipReason.OverlappingCandidate -> "overlapping-candidate"
    }

private data class RuleTraceCollector(
    val traces: MutableList<FileRuleTrace> = mutableListOf(),
) {
    /**
     * Records the rule IDs that were considered for one file.
     */
    public fun record(
        databaseName: String,
        filePath: String,
        ruleIds: List<QualifiedRuleId>,
    ) {
        traces += FileRuleTrace(databaseName = databaseName, filePath = filePath, ruleIds = ruleIds)
    }
}

private data class FileRuleTrace(
    val databaseName: String,
    val filePath: String,
    val ruleIds: List<QualifiedRuleId>,
)

internal fun diagnosticRuleHitsByFile(diagnostics: List<Diagnostic>): Map<FileRuleKey, Set<String>> =
    diagnostics
        .filter { diagnostic -> diagnostic.file != null && diagnostic.database != null }
        .groupBy { diagnostic ->
            FileRuleKey(
                databaseName = diagnostic.database!!.name,
                filePath = diagnostic.file!!.path,
            )
        }
        .mapValues { (_, fileDiagnostics) ->
            fileDiagnostics.mapTo(linkedSetOf()) { diagnostic -> diagnostic.ruleId.value }
        }

internal data class FileRuleKey(
    val databaseName: String,
    val filePath: String,
)
