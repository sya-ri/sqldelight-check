package dev.s7a.sqldelight.check.gradle

import dev.s7a.sqldelight.check.api.DatabaseContext
import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.LogLevel
import dev.s7a.sqldelight.check.api.QualifiedRuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SourceFile
import dev.s7a.sqldelight.check.core.AnalysisInput
import dev.s7a.sqldelight.check.core.AnalysisPhase
import dev.s7a.sqldelight.check.core.AnalysisTrace
import dev.s7a.sqldelight.check.core.Baseline
import dev.s7a.sqldelight.check.core.CheckConfig
import dev.s7a.sqldelight.check.core.DatabaseConfig
import dev.s7a.sqldelight.check.core.FixApplier
import dev.s7a.sqldelight.check.core.FixSkipReason
import dev.s7a.sqldelight.check.core.ReporterRegistry
import dev.s7a.sqldelight.check.core.RuleRegistry
import dev.s7a.sqldelight.check.core.SqlDelightCheckEngine
import dev.s7a.sqldelight.check.reporter.api.Report
import dev.s7a.sqldelight.check.reporter.api.ReportOutput
import dev.s7a.sqldelight.check.rule.api.RuleDeprecation
import dev.s7a.sqldelight.check.rule.api.RuleOptionDeprecation
import java.io.File
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/**
 * Base task for sqldelight-check operations.
 *
 * All project state is captured into task properties at configuration time so that
 * the task action runs without any `project` access, enabling configuration cache
 * compatibility.
 */
@DisableCachingByDefault(because = "The fix task can rewrite source files, and reporters may write configurable outputs.")
public abstract class SqlDelightCheckTask : AbstractSqlDelightCheckBaseTask() {
    /**
     * Whether this task should apply allowed fixes to source files.
     */
    @get:Input
    public abstract val applyFixes: Property<Boolean>

    /**
     * Whether this task should collect and log rule execution metrics.
     */
    @get:Input
    public abstract val performanceMetrics: Property<Boolean>

    /**
     * Optional baseline file containing known diagnostics to suppress.
     */
    @get:Optional
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    public abstract val baselineFile: RegularFileProperty

    // ── classpath inputs ────────────────────────────────────────────────────

    /**
     * Runtime classpath used to discover reporter providers.
     */
    @get:Classpath
    public abstract val reporterClasspath: ConfigurableFileCollection

    /**
     * Whether fix tasks may apply unsafe fixes.
     */
    @get:Input
    public abstract val allowUnsafeFixes: Property<Boolean>

    // ── reporter outputs ─────────────────────────────────────────────────────

    /**
     * Reporter configurations, captured at configuration time.
     */
    @get:Nested
    public abstract val reporters: ListProperty<SqlDelightReporterTaskSpec>

    /**
     * Default directory where built-in reports are written.
     */
    @get:OutputDirectory
    public abstract val reportOutputDirectory: DirectoryProperty

    // ── path resolution inputs ───────────────────────────────────────────────

    /**
     * Absolute path of this project's directory, for fix-mode file resolution.
     */
    @get:Internal
    public abstract val projectDir: Property<String>

    // ── task action ───────────────────────────────────────────────────────────

    @TaskAction
    public fun run() {
        val logLevel = logLevel.get()
        val baseline = baselineFile.orNull?.asFile?.readSqldelightCheckBaseline() ?: Baseline.Empty
        val config =
            buildCheckConfig(
                globalRuleSets = globalRuleSets.get(),
                globalRules = globalRules.get(),
                databaseConfigs = databaseConfigs.get(),
                allowUnsafeFixes = allowUnsafeFixes.get(),
                logLevel = logLevel,
                baseline = baseline,
            )

        val traceCollector = RuleTraceCollector()
        val performanceMetricsCollector = PerformanceMetricsCollector()
        val trace = tracing(logLevel, traceCollector, performanceMetrics.get(), performanceMetricsCollector)

        val ruleRegistry = buildRuleRegistry()
        val dialectRegistry = buildDialectRegistry()
        val analysisInputs = buildAnalysisInputs(dialectRegistry)

        validateConfiguredRules(config, ruleRegistry)

        var result = runAnalysis(config, trace, ruleRegistry, analysisInputs)

        if (applyFixes.get()) {
            val fixResult = applyDiagnosticFixes(result.diagnostics, config.allowUnsafeFixes)
            if (fixResult.changedFiles > 0) {
                logger.lifecycle("Applied sqldelight-check fixes to {} file(s).", fixResult.changedFiles)
                // Re-read file contents after fixes and re-analyze.
                val refreshedInputs = buildAnalysisInputs(dialectRegistry)
                result = runAnalysis(config, trace, ruleRegistry, refreshedInputs)
            }
            if (fixResult.skippedReasons.isNotEmpty()) {
                val skippedSummary =
                    fixResult.skippedReasons.entries.joinToString(", ") { (reason, count) ->
                        "${reason.logLabel()}=$count"
                    }
                logger.lifecycle("Skipped sqldelight-check fixes: {}.", skippedSummary)
            }
        }

        writeReports(result.diagnostics)
        logDiagnostics(result.diagnostics)
        logRuleHits(logLevel, traceCollector.traces, result.diagnostics)
        logPerformanceMetrics(performanceMetricsCollector)
        logger.lifecycle("sqldelight-check analyzed {} SQLDelight database(s).", result.databaseCount)

        val errorCount = result.diagnostics.count { it.severity == Severity.Error }
        if (errorCount > 0) {
            throw GradleException("sqldelight-check found $errorCount error diagnostic(s).")
        }
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private fun runAnalysis(
        config: CheckConfig,
        trace: AnalysisTrace,
        ruleRegistry: RuleRegistry,
        inputs: List<AnalysisInput>,
    ): AnalysisRunResult {
        val diagnostics =
            SqlDelightCheckEngine().run(
                inputs = inputs,
                ruleSetProviders = ruleRegistry.providers(),
                config = config,
                trace = trace,
            )
        return AnalysisRunResult(databaseCount = inputs.size, diagnostics = diagnostics)
    }

    private fun tracing(
        logLevel: LogLevel,
        traceCollector: RuleTraceCollector,
        performanceMetricsEnabled: Boolean,
        performanceMetricsCollector: PerformanceMetricsCollector,
    ): AnalysisTrace =
        object : LoggingAnalysisTrace(logLevel, logger) {
            override val collectsPerformanceMetrics: Boolean = performanceMetricsEnabled

            override fun fileRules(
                database: DatabaseContext,
                file: SourceFile,
                ruleIds: List<QualifiedRuleId>,
            ) {
                if (!logLevel.logsRules) return
                traceCollector.record(database.name, file.path, ruleIds)
            }

            override fun ruleTiming(
                database: DatabaseContext,
                file: SourceFile,
                ruleId: QualifiedRuleId,
                durationNanos: Long,
            ) {
                performanceMetricsCollector.recordRule(database.name, ruleId, durationNanos)
            }

            override fun analysisPhaseTiming(
                database: DatabaseContext,
                file: SourceFile,
                phase: AnalysisPhase,
                durationNanos: Long,
            ) {
                performanceMetricsCollector.recordPhase(database.name, phase, durationNanos)
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

    private fun logPerformanceMetrics(collector: PerformanceMetricsCollector) {
        if (!performanceMetrics.get()) return

        logger.lifecycle("sqldelight-check performance metrics (slowest rules first):")
        collector.rules
            .entries
            .sortedWith(
                compareByDescending<Map.Entry<RuleTimingKey, TimingAggregate>> { entry -> entry.value.totalNanos }
                    .thenBy { entry -> entry.key.databaseName }
                    .thenBy { entry -> entry.key.ruleId.value },
            )
            .forEach { (key, timing) ->
                logger.lifecycle(
                    "sqldelight-check performance rule [{}] {} invocations={} total={} avg={} max={}",
                    key.databaseName,
                    key.ruleId.value,
                    timing.invocations,
                    timing.totalNanos.formatDuration(),
                    (timing.totalNanos / timing.invocations).formatDuration(),
                    timing.maximumNanos.formatDuration(),
                )
            }
        if (collector.phases.isNotEmpty()) {
            logger.lifecycle("sqldelight-check performance metrics (shared phases):")
            collector.phases
                .entries
                .sortedWith(
                    compareByDescending<Map.Entry<PhaseTimingKey, TimingAggregate>> { entry -> entry.value.totalNanos }
                        .thenBy { entry -> entry.key.databaseName }
                        .thenBy { entry -> entry.key.phase.name },
                )
                .forEach { (key, timing) ->
                    logger.lifecycle(
                        "sqldelight-check performance phase [{}] {} invocations={} total={} avg={} max={}",
                        key.databaseName,
                        key.phase.name.lowercase(),
                        timing.invocations,
                        timing.totalNanos.formatDuration(),
                        (timing.totalNanos / timing.invocations).formatDuration(),
                        timing.maximumNanos.formatDuration(),
                    )
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
            .filter { it.file != null }
            .groupBy { it.file?.path.orEmpty() }
            .forEach { (path, fileDiagnostics) ->
                val file = resolveSourceFile(path) ?: return@forEach

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

    private fun resolveSourceFile(path: String): File? {
        val relativePath = path.normalizedRelativePath() ?: return null
        val reportRootFile = reportRoot.orNull?.takeIf { it.isNotBlank() }?.let { File(it) }
        return sequenceOf(
            reportRootFile?.resolve(relativePath.toString())?.normalize(),
            File(rootProjectDir.get()).resolve(relativePath.toString()).normalize(),
            File(projectDir.get()).resolve(relativePath.toString()).normalize(),
        ).firstOrNull { file -> file?.isFile == true }
    }

    private fun writeReports(diagnostics: List<Diagnostic>) {
        val report = Report(diagnostics)
        val registry = ReporterRegistry.load(buildPluginClassLoader(reporterClasspath))

        reporters.get()
            .filter { it.required.get() }
            .forEach { spec ->
                val provider =
                    registry.find(spec.name.get())
                        ?: throw GradleException("sqldelight-check reporter '${spec.name.get()}' was not found on the runtime classpath.")
                val outputFile = spec.primaryOutputFileAsFile()
                val outputDirectory = spec.outputDirectoryAsFile()
                provider
                    .create(spec.options.get())
                    .write(
                        report,
                        GradleReportOutput(primaryFile = outputFile, outputDirectory = outputDirectory),
                    )
                logger.lifecycle("Wrote sqldelight-check {} report to {}", spec.name.get(), outputFile)
            }
    }

    private fun logDiagnostics(diagnostics: List<Diagnostic>) {
        diagnostics.forEach { diagnostic ->
            val message =
                "sqldelight-check ${diagnostic.severity.logLabel()} ${diagnostic.ruleId.value} at ${diagnostic.locationLabel()}: ${diagnostic.message}"
            when (diagnostic.severity) {
                Severity.Info -> logger.info(message)
                Severity.Warning -> logger.warn(message)
                Severity.Error -> logger.error(message)
            }
        }
    }
}

// ── private output helper ─────────────────────────────────────────────────────

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

// ── private data classes ──────────────────────────────────────────────────────

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

private fun Severity.logLabel(): String =
    when (this) {
        Severity.Info -> "info"
        Severity.Warning -> "warning"
        Severity.Error -> "error"
    }

internal fun deprecatedRuleMessage(
    database: DatabaseContext,
    ruleId: QualifiedRuleId,
    deprecation: RuleDeprecation,
    enabled: Boolean,
): String =
    buildString {
        append("sqldelight-check deprecated rule ")
        append(ruleId.value)
        append(" is explicitly ")
        append(if (enabled) "enabled" else "disabled")
        append(" for database ")
        append(database.name)
        append(". ")
        append(deprecation.message)
        deprecation.replacement?.let { replacement ->
            append(" Use ")
            append(replacement.value)
            append(" instead.")
        }
        if (!enabled) {
            append(" Remove this rule configuration.")
        }
    }

internal fun unknownRuleOptionMessage(
    database: DatabaseContext,
    ruleId: QualifiedRuleId,
    optionName: String,
    knownOptionNames: Set<String>,
): String =
    buildString {
        append("sqldelight-check unknown option ")
        append(optionName)
        append(" for rule ")
        append(ruleId.value)
        append(" in database ")
        append(database.name)
        append(".")
        if (knownOptionNames.isNotEmpty()) {
            append(" Known options: ")
            append(knownOptionNames.sorted().joinToString(", "))
            append(".")
        }
    }

internal fun deprecatedRuleOptionMessage(
    database: DatabaseContext,
    ruleId: QualifiedRuleId,
    optionName: String,
    deprecation: RuleOptionDeprecation,
): String =
    buildString {
        append("sqldelight-check deprecated option ")
        append(optionName)
        append(" for rule ")
        append(ruleId.value)
        append(" is configured for database ")
        append(database.name)
        append(". ")
        append(deprecation.message)
        deprecation.replacement?.let { replacement ->
            append(" Use ")
            append(replacement)
            append(" instead.")
        }
    }

private fun Diagnostic.locationLabel(): String =
    buildString {
        append(file?.path ?: "<unknown>")
        val range = range ?: return@buildString
        append(':')
        append(range.start.line)
        append(':')
        append(range.start.column)
        append('-')
        append(range.end.line)
        append(':')
        append(range.end.column)
    }

private fun String.normalizedRelativePath(): Path? {
    val path = runCatching { Path.of(this).normalize() }.getOrNull() ?: return null
    if (path.isAbsolute || path.startsWith("..")) return null
    return path
}

private class RuleTraceCollector {
    val traces: CopyOnWriteArrayList<FileRuleTrace> = CopyOnWriteArrayList()

    fun record(
        databaseName: String,
        filePath: String,
        ruleIds: List<QualifiedRuleId>,
    ) {
        traces += FileRuleTrace(databaseName = databaseName, filePath = filePath, ruleIds = ruleIds)
    }
}

private class PerformanceMetricsCollector {
    val rules: ConcurrentHashMap<RuleTimingKey, TimingAggregate> = ConcurrentHashMap()
    val phases: ConcurrentHashMap<PhaseTimingKey, TimingAggregate> = ConcurrentHashMap()

    fun recordRule(
        databaseName: String,
        ruleId: QualifiedRuleId,
        durationNanos: Long,
    ) {
        rules.computeIfAbsent(RuleTimingKey(databaseName, ruleId)) { TimingAggregate() }.record(durationNanos)
    }

    fun recordPhase(
        databaseName: String,
        phase: AnalysisPhase,
        durationNanos: Long,
    ) {
        phases.computeIfAbsent(PhaseTimingKey(databaseName, phase)) { TimingAggregate() }.record(durationNanos)
    }
}

private data class RuleTimingKey(
    val databaseName: String,
    val ruleId: QualifiedRuleId,
)

private data class PhaseTimingKey(
    val databaseName: String,
    val phase: AnalysisPhase,
)

private class TimingAggregate {
    private val _invocations = AtomicInteger(0)
    private val _totalNanos = AtomicLong(0L)
    private val _maximumNanos = AtomicLong(0L)

    val invocations: Int get() = _invocations.get()
    val totalNanos: Long get() = _totalNanos.get()
    val maximumNanos: Long get() = _maximumNanos.get()

    fun record(durationNanos: Long) {
        _invocations.incrementAndGet()
        _totalNanos.addAndGet(durationNanos)
        _maximumNanos.accumulateAndGet(durationNanos, ::maxOf)
    }
}

private fun Long.formatDuration(): String =
    "%.3fms".format(java.util.Locale.ROOT, this / 1_000_000.0)

private data class FileRuleTrace(
    val databaseName: String,
    val filePath: String,
    val ruleIds: List<QualifiedRuleId>,
)

internal fun diagnosticRuleHitsByFile(diagnostics: List<Diagnostic>): Map<FileRuleKey, Set<String>> =
    diagnostics
        .filter { it.file != null && it.database != null }
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

internal fun validateConfiguredRules(
    config: CheckConfig,
    registry: RuleRegistry,
) {
    val configuredRuleSetIds =
        (config.ruleSets.keys + config.databases.values.flatMap(DatabaseConfig::ruleSetIds))
            .mapTo(linkedSetOf()) { ruleSetId -> ruleSetId }
    val unknownRuleSetIds = configuredRuleSetIds - registry.ruleSetIds()
    require(unknownRuleSetIds.isEmpty()) {
        "Unknown sqldelight-check rule set ID(s): ${unknownRuleSetIds.joinToString { ruleSetId -> ruleSetId.value }}"
    }

    val configuredRuleIds =
        (config.rules.keys + config.databases.values.flatMap(DatabaseConfig::ruleIds))
            .mapTo(linkedSetOf()) { ruleId -> ruleId }
    val unknownRuleIds = configuredRuleIds - registry.ruleIds()
    require(unknownRuleIds.isEmpty()) {
        "Unknown sqldelight-check rule ID(s): ${unknownRuleIds.joinToString { ruleId -> ruleId.value }}"
    }
}

private fun DatabaseConfig.ruleSetIds() = ruleSets.keys

private fun DatabaseConfig.ruleIds() = rules.keys
