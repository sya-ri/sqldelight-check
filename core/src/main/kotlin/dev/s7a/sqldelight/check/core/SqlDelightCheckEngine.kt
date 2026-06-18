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
import dev.s7a.sqldelight.check.rule.api.DiagnosticRefinement
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext
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
        rules.forEach { candidate ->
            val ruleConfig =
                resolver.resolveRule(
                    ruleId = candidate.ruleId,
                    databaseName = database.name,
                    defaultEnabled = candidate.rule.defaultEnabled,
                    defaultSeverity = candidate.rule.defaultSeverity,
                )
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
        return files.flatMap { file -> runRulesForFile(database, file, rules, refinementsByRuleId, resolver, trace) }
    }

    private fun runRulesForFile(
        database: DatabaseContext,
        file: SourceFile,
        rules: List<RuleCandidate>,
        refinementsByRuleId: Map<QualifiedRuleId, List<DiagnosticRefinement>>,
        resolver: ConfigurationResolver,
        trace: AnalysisTrace,
    ): List<Diagnostic> {
        val facts = SourceSqlFactsExtractor.extract(file, database.dialect)
        val disableDirectives = DisableDirectives.parse(file)
        val executedRuleIds = mutableListOf<QualifiedRuleId>()
        val diagnostics =
            rules.flatMap { candidate ->
                val ruleSetConfig = resolver.resolveRuleSet(candidate.ruleSetId, database.name)
                val ruleConfig =
                    resolver.resolveRule(
                        ruleId = candidate.ruleId,
                        databaseName = database.name,
                        defaultEnabled = candidate.rule.defaultEnabled,
                        defaultSeverity = candidate.rule.defaultSeverity,
                    )
                val context =
                    object : RuleContext {
                        override val database: DatabaseContext = database
                        override val file: SourceFile = file
                        override val options: RuleOptions = RuleOptions(ruleConfig.options)
                        override val facts: SqlFacts = facts
                    }
                val enablement =
                    ruleConfig.enablement ?: ruleSetConfig.enablement
                if (!candidate.rule.shouldRun(context, enablement)) return@flatMap emptyList()

                executedRuleIds += candidate.ruleId
                val diagnostics = mutableListOf<Diagnostic>()
                candidate.rule.run(context) { diagnostic ->
                    diagnostic
                        .withRuleIdentity(candidate.ruleId, ruleConfig.severity)
                        .applyRefinements(context, refinementsByRuleId[candidate.ruleId].orEmpty())
                        ?.let(diagnostics::add)
                }
                diagnostics
            }.filterNot(disableDirectives::suppresses)
        trace.fileRules(database, file, executedRuleIds)
        return diagnostics +
            coreRuleSeverity(resolver, database.name, coreRequireSuppressionReasonRuleId).orEmptyDiagnostics { severity ->
                disableDirectives.suppressionReasonDiagnostics(
                    file = file,
                    ruleId = coreRequireSuppressionReasonRuleId,
                    severity = severity,
                    database = database,
                )
            } +
            coreRuleSeverity(resolver, database.name, coreNoRedundantSuppressionRuleId).orEmptyDiagnostics { severity ->
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

private data class RuleCandidate(
    val ruleSetId: RuleSetId,
    val ruleId: QualifiedRuleId,
    val rule: Rule,
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
