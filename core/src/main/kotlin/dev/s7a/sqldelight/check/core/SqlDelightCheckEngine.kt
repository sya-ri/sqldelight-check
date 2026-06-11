package dev.s7a.sqldelight.check.core

import dev.s7a.sqldelight.check.api.DatabaseContext
import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.RuleSetId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SourceFile
import dev.s7a.sqldelight.check.core.sqldelight.SqlDelight2Analyzer
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext
import dev.s7a.sqldelight.check.rule.api.RuleSetProvider
import dev.s7a.sqldelight.check.rule.api.SqlFacts
import dev.s7a.sqldelight.check.rule.api.defaultEnablement

/**
 * Runs SQLDelight analysis and sqldelight-check rules for resolved database inputs.
 *
 * The engine keeps Gradle- and reporter-specific behavior outside the core
 * execution path so tests and custom integrations can exercise the same
 * analysis model directly.
 */
public class SqlDelightCheckEngine {
    /**
     * Runs analysis for all resolved SQLDelight databases and returns every
     * diagnostic that remains after source-level rule suppressions are applied.
     */
    public fun run(
        inputs: List<AnalysisInput> = emptyList(),
        ruleSetProviders: List<RuleSetProvider> = emptyList(),
        config: CheckConfig = CheckConfig(),
        trace: AnalysisTrace = AnalysisTrace.None,
    ): List<Diagnostic> =
        inputs.flatMap { input ->
            val analysisResult = SqlDelight2Analyzer.analyze(input)
            trace.databaseFiles(input.database, analysisResult.files)
            analysisResult.diagnostics + runRules(input.database, analysisResult.files, ruleSetProviders, config, trace)
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
                    RuleCandidate(provider.id, rule.id.toFullRuleId(provider.id), rule)
                }
            }
        return files.flatMap { file -> runRulesForFile(database, file, rules, resolver, trace) }
    }

    private fun runRulesForFile(
        database: DatabaseContext,
        file: SourceFile,
        rules: List<RuleCandidate>,
        resolver: ConfigurationResolver,
        trace: AnalysisTrace,
    ): List<Diagnostic> {
        val facts = SourceSqlFactsExtractor.extract(file)
        val disableDirectives = DisableDirectives.parse(file)
        val executedRuleIds = mutableListOf<RuleId>()
        val diagnostics =
            rules.flatMap { candidate ->
                val ruleSetConfig = resolver.resolveRuleSet(candidate.ruleSetId, database.name)
                val ruleConfig =
                    resolver.resolveRule(
                        ruleId = candidate.ruleId,
                        databaseName = database.name,
                        defaultEnablement = candidate.rule.defaultEnablement,
                        defaultSeverity = candidate.rule.defaultSeverity,
                    )
                val context =
                    object : RuleContext {
                        override val database: DatabaseContext = database
                        override val file: SourceFile = file
                        override val options: Map<String, String> = ruleConfig.options
                        override val facts: SqlFacts = facts
                    }
                val enablement = EnablementResolver.resolveRuleEnablement(ruleSetConfig.enablement, ruleConfig.enablement)
                if (!candidate.rule.shouldRun(context, enablement)) return@flatMap emptyList()

                executedRuleIds += candidate.ruleId
                val diagnostics = mutableListOf<Diagnostic>()
                candidate.rule.run(
                    context,
                    DiagnosticReporter { diagnostic ->
                        diagnostics += diagnostic.withRuleIdentity(candidate.ruleId, ruleConfig.severity)
                    },
                )
                diagnostics
            }.filterNot(disableDirectives::suppresses)
        trace.fileRules(database, file, executedRuleIds)
        return diagnostics
    }

    private fun Rule.shouldRun(
        context: RuleContext,
        enablement: Enablement,
    ): Boolean =
        when (enablement) {
            Enablement.Enabled -> true
            Enablement.Disabled -> false
            Enablement.Auto -> hasTargetCapability(context) && isApplicable(context)
        }

    private fun Rule.hasTargetCapability(context: RuleContext): Boolean =
        targetCapability?.let { capability -> capability in context.database.dialect.capabilities } ?: true
}

private fun Diagnostic.withRuleIdentity(
    ruleId: RuleId,
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

private data class RuleCandidate(
    val ruleSetId: RuleSetId,
    val ruleId: RuleId,
    val rule: Rule,
)

private fun String.toFullRuleId(ruleSetId: RuleSetId): RuleId {
    require(':' !in this) {
        "Rule ID must be local and must not contain ':': ${ruleSetId.value}:$this"
    }
    return RuleId("${ruleSetId.value}:$this")
}
