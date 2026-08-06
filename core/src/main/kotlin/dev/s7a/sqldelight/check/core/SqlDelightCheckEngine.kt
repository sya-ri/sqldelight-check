@file:OptIn(InternalSqldelightCheckApi::class)

package dev.s7a.sqldelight.check.core

import dev.s7a.sqldelight.check.api.DatabaseContext
import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.InternalSqldelightCheckApi
import dev.s7a.sqldelight.check.api.QualifiedRuleId
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleSetId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SourceFile
import dev.s7a.sqldelight.check.api.SqlSourceStructure
import dev.s7a.sqldelight.check.rule.api.DiagnosticRefinement
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext
import dev.s7a.sqldelight.check.rule.api.RuleDeprecation
import dev.s7a.sqldelight.check.rule.api.RuleOptionDeprecation
import dev.s7a.sqldelight.check.rule.api.RuleOptions
import dev.s7a.sqldelight.check.rule.api.RuleSetProvider
import dev.s7a.sqldelight.check.rule.api.SqlFacts

/**
 * Runs sqldelight-check rules for resolved database inputs.
 *
 * The engine keeps Gradle- and reporter-specific behavior outside the core
 * execution path so tests and custom integrations can exercise the same
 * rule model directly.
 */
public class SqlDelightCheckEngine {
    /**
     * Runs rules for all resolved SQLDelight databases and returns every
     * diagnostic that remains after source-level rule suppressions are applied.
     */
    public fun run(
        inputs: List<AnalysisInput> = emptyList(),
        ruleSetProviders: List<RuleSetProvider> = emptyList(),
        config: CheckConfig = CheckConfig(),
        trace: AnalysisTrace = AnalysisTrace.None,
    ): List<Diagnostic> =
        inputs.flatMap { input ->
            trace.databaseFiles(input.database, input.files)
            runRules(input.database, input.files, ruleSetProviders, config, trace)
        }

    private fun runRules(
        database: DatabaseContext,
        files: List<SourceFile>,
        ruleSetProviders: List<RuleSetProvider>,
        config: CheckConfig,
        trace: AnalysisTrace,
    ): List<Diagnostic> {
        val resolver = ConfigurationResolver(config)
        val rules =
            ruleSetProviders.flatMap { provider ->
                provider.ruleProviders().map { ruleProvider ->
                    val rule = ruleProvider.create()
                    RuleCandidate(provider.id, QualifiedRuleId(provider.id, rule.id), rule)
                }
            }
        val refinementsByRuleId =
            ruleSetProviders
                .flatMap { provider -> provider.diagnosticRefinementProviders() }
                .map { refinementProvider -> refinementProvider.create() }
                .groupBy { refinement -> refinement.targetRuleId }
        val resolvedRules =
            rules.map { candidate ->
                val ruleSetConfig = resolver.resolveRuleSet(candidate.ruleSetId, database.name)
                val ruleConfig =
                    resolver.resolveRule(
                        ruleId = candidate.ruleId,
                        databaseName = database.name,
                        defaultEnabled = candidate.rule.defaultEnabled,
                        defaultSeverity = candidate.rule.defaultSeverity,
                    )
                ResolvedRuleCandidate(candidate, ruleSetConfig, ruleConfig)
            }
        resolvedRules.forEach { candidate ->
            val ruleConfig = candidate.ruleConfig
            candidate.rule.deprecation?.let { deprecation ->
                if (ruleConfig.explicitlyConfigured && ruleConfig.enablement != null) {
                    trace.deprecatedRule(
                        database = database,
                        ruleId = candidate.ruleId,
                        deprecation = deprecation,
                        enabled = ruleConfig.enablement,
                    )
                }
            }
            traceRuleOptionConfiguration(database, candidate.ruleId, candidate.rule, ruleConfig, trace)
        }
        val coreSeverities =
            CoreRuleSeverities(
                requireSuppressionReason = coreRuleSeverity(resolver, database.name, coreRequireSuppressionReasonRuleId),
                noRedundantSuppression = coreRuleSeverity(resolver, database.name, coreNoRedundantSuppressionRuleId),
            )
        // Wrap trace so that callbacks invoked from parallel worker threads are serialised.
        val concurrentTrace = ConcurrentAnalysisTrace(trace)
        return files.parallelStream()
            .flatMap { file ->
                runRulesForFile(database, file, resolvedRules, refinementsByRuleId, coreSeverities, config, concurrentTrace)
                    .stream()
            }
            .toList()
    }

    private fun runRulesForFile(
        database: DatabaseContext,
        file: SourceFile,
        rules: List<ResolvedRuleCandidate>,
        refinementsByRuleId: Map<QualifiedRuleId, List<DiagnosticRefinement>>,
        coreSeverities: CoreRuleSeverities,
        config: CheckConfig,
        trace: AnalysisTrace,
    ): List<Diagnostic> {
        val phaseTrace =
            if (trace.collectsPerformanceMetrics) {
                { phase: AnalysisPhase, durationNanos: Long ->
                    trace.analysisPhaseTiming(database, file, phase, durationNanos)
                }
            } else {
                null
            }
        val facts = SourceSqlFactsExtractor.extract(file, database.dialect, phaseTrace)
        val disableDirectives = DisableDirectives.parse(file)
        val lazySourceStructure = lazy { SqlSourceStructure.parse(file.content, database.dialect.sourcePatterns) }
        // Run rules in parallel; encounter order is preserved by parallelStream on an ordered source.
        // Rules must be stateless: rule instances are shared across files within the same engine run.
        val outcomes =
            rules.parallelStream()
                .map { candidate ->
                    val ruleSetConfig = candidate.ruleSetConfig
                    val ruleConfig = candidate.ruleConfig
                    val context =
                        object : RuleContext {
                            override val database: DatabaseContext = database
                            override val file: SourceFile = file
                            override val options: RuleOptions = RuleOptions(ruleConfig.options)
                            override val facts: SqlFacts = facts
                            override val sourceStructure: SqlSourceStructure
                                get() = lazySourceStructure.value
                        }
                    val enablement = ruleConfig.enablement ?: ruleSetConfig.enablement
                    if (!candidate.rule.shouldRun(context, enablement)) return@map RuleOutcome(null, emptyList())

                    val diagnostics = mutableListOf<Diagnostic>()
                    val startNanos = if (trace.collectsPerformanceMetrics) System.nanoTime() else 0L
                    try {
                        candidate.rule.run(context) { diagnostic ->
                            diagnostic
                                .withRuleIdentity(candidate.ruleId, ruleConfig.severity)
                                .applyRefinements(context, refinementsByRuleId[candidate.ruleId].orEmpty())
                                ?.let(diagnostics::add)
                        }
                    } finally {
                        if (trace.collectsPerformanceMetrics) {
                            trace.ruleTiming(database, file, candidate.ruleId, System.nanoTime() - startNanos)
                        }
                    }
                    RuleOutcome(candidate.ruleId, diagnostics)
                }
                .toList()
        val executedRuleIds = outcomes.mapNotNull { it.ruleId }
        val diagnostics =
            outcomes
                .flatMap { it.diagnostics }
                .filterNot { diagnostic ->
                    disableDirectives.suppresses(diagnostic) || config.baseline.suppresses(diagnostic)
                }
        trace.fileRules(database, file, executedRuleIds)
        return diagnostics +
            coreSeverities.requireSuppressionReason.orEmptyDiagnostics { severity ->
                disableDirectives.suppressionReasonDiagnostics(
                    file = file,
                    ruleId = coreRequireSuppressionReasonRuleId,
                    severity = severity,
                    database = database,
                )
            } +
            coreSeverities.noRedundantSuppression.orEmptyDiagnostics { severity ->
                disableDirectives.redundantDisableDiagnostics(
                    file = file,
                    ruleId = coreNoRedundantSuppressionRuleId,
                    severity = severity,
                    database = database,
                )
            }
    }

    private fun Rule.shouldRun(
        context: RuleContext,
        enablement: Boolean?,
    ): Boolean =
        when (enablement) {
            true -> true
            false -> false
            null -> deprecation == null && hasTargetDialect(context) && isApplicable(context)
        }

    private fun Rule.hasTargetDialect(context: RuleContext): Boolean =
        targetDialect?.let { id -> id in context.database.dialect.ids } ?: true
}

private fun traceRuleOptionConfiguration(
    database: DatabaseContext,
    ruleId: QualifiedRuleId,
    rule: Rule,
    ruleConfig: ResolvedRuleConfig,
    trace: AnalysisTrace,
) {
    if (ruleConfig.options.isEmpty()) return

    val declaredOptions = rule.options.associateBy { option -> option.name }
    val declaredOptionNames = declaredOptions.keys
    ruleConfig.options.keys.forEach { optionName ->
        val option = declaredOptions[optionName]
        if (option == null) {
            trace.unknownRuleOption(
                database = database,
                ruleId = ruleId,
                optionName = optionName,
                knownOptionNames = declaredOptionNames,
            )
            return@forEach
        }
        option.deprecation?.let { deprecation ->
            trace.deprecatedRuleOption(
                database = database,
                ruleId = ruleId,
                optionName = optionName,
                deprecation = deprecation,
            )
        }
    }
}

private fun RuleDiagnostic.withRuleIdentity(
    ruleId: QualifiedRuleId,
    severity: Severity,
): Diagnostic =
    Diagnostic(
        ruleId = ruleId,
        severity = severity,
        message = message,
        file = file,
        range = range,
        database = database,
        fixes = fixes,
    )

private fun Diagnostic.applyRefinements(
    context: RuleContext,
    refinements: List<DiagnosticRefinement>,
): Diagnostic? =
    refinements.fold(this as Diagnostic?) { current, refinement ->
        current?.let { diagnostic -> refinement.refine(context, diagnostic) }
    }

private data class RuleOutcome(
    val ruleId: QualifiedRuleId?,
    val diagnostics: List<Diagnostic>,
)

private data class RuleCandidate(
    val ruleSetId: RuleSetId,
    val ruleId: QualifiedRuleId,
    val rule: Rule,
)

private data class ResolvedRuleCandidate(
    val candidate: RuleCandidate,
    val ruleSetConfig: ResolvedRuleSetConfig,
    val ruleConfig: ResolvedRuleConfig,
) {
    val ruleSetId: RuleSetId get() = candidate.ruleSetId
    val ruleId: QualifiedRuleId get() = candidate.ruleId
    val rule: Rule get() = candidate.rule
}

private data class CoreRuleSeverities(
    val requireSuppressionReason: Severity?,
    val noRedundantSuppression: Severity?,
)

private fun coreRuleSeverity(
    resolver: ConfigurationResolver,
    databaseName: String,
    ruleId: QualifiedRuleId,
): Severity? {
    val ruleSetConfig = resolver.resolveRuleSet(coreRuleSetId, databaseName, defaultEnabled = true)
    val ruleConfig =
        resolver.resolveRule(
            ruleId = ruleId,
            databaseName = databaseName,
            defaultEnabled = true,
            defaultSeverity = Severity.Warning,
        )
    val enablement = ruleConfig.enablement ?: ruleSetConfig.enablement
    return if (enablement == false) null else ruleConfig.severity
}

private val Rule.defaultEnabled: Boolean?
    get() = if (defaultEnable) null else false

private inline fun Severity?.orEmptyDiagnostics(block: (Severity) -> List<Diagnostic>): List<Diagnostic> =
    this?.let(block).orEmpty()

/**
 * Wraps an [AnalysisTrace] so that callbacks invoked from parallel worker threads are serialised,
 * letting callers implement [AnalysisTrace] without worrying about concurrent access.
 *
 * Only the callbacks called during parallel execution ([fileRules], [ruleTiming],
 * [analysisPhaseTiming]) are synchronised; the rest are called sequentially before and after the
 * parallel region and are forwarded directly.
 */
private class ConcurrentAnalysisTrace(private val delegate: AnalysisTrace) : AnalysisTrace {
    override val collectsPerformanceMetrics: Boolean = delegate.collectsPerformanceMetrics

    override fun databaseFiles(database: DatabaseContext, files: List<SourceFile>) =
        delegate.databaseFiles(database, files)

    @Synchronized
    override fun fileRules(database: DatabaseContext, file: SourceFile, ruleIds: List<QualifiedRuleId>) =
        delegate.fileRules(database, file, ruleIds)

    @Synchronized
    override fun ruleTiming(database: DatabaseContext, file: SourceFile, ruleId: QualifiedRuleId, durationNanos: Long) =
        delegate.ruleTiming(database, file, ruleId, durationNanos)

    @Synchronized
    override fun analysisPhaseTiming(database: DatabaseContext, file: SourceFile, phase: AnalysisPhase, durationNanos: Long) =
        delegate.analysisPhaseTiming(database, file, phase, durationNanos)

    override fun deprecatedRule(database: DatabaseContext, ruleId: QualifiedRuleId, deprecation: RuleDeprecation, enabled: Boolean) =
        delegate.deprecatedRule(database, ruleId, deprecation, enabled)

    override fun unknownRuleOption(database: DatabaseContext, ruleId: QualifiedRuleId, optionName: String, knownOptionNames: Set<String>) =
        delegate.unknownRuleOption(database, ruleId, optionName, knownOptionNames)

    override fun deprecatedRuleOption(database: DatabaseContext, ruleId: QualifiedRuleId, optionName: String, deprecation: RuleOptionDeprecation) =
        delegate.deprecatedRuleOption(database, ruleId, optionName, deprecation)
}

private val coreRuleSetId = RuleSetId("core")

private val coreRequireSuppressionReasonRuleId =
    QualifiedRuleId(
        ruleSetId = coreRuleSetId,
        ruleId = RuleId("require-suppression-reason"),
    )

private val coreNoRedundantSuppressionRuleId =
    QualifiedRuleId(
        ruleSetId = coreRuleSetId,
        ruleId = RuleId("no-redundant-suppression"),
    )
