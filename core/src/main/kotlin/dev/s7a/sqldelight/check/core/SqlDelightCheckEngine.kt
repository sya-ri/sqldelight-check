package dev.s7a.sqldelight.check.core

import dev.s7a.sqldelight.check.api.DatabaseContext
import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.RuleSetId
import dev.s7a.sqldelight.check.api.SourceFile
import dev.s7a.sqldelight.check.core.sqldelight.SqlDelight2Analyzer
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext
import dev.s7a.sqldelight.check.rule.api.RuleSetProvider
import dev.s7a.sqldelight.check.rule.api.SqlFacts

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
    ): List<Diagnostic> =
        inputs.flatMap { input ->
            val analysisResult = SqlDelight2Analyzer.analyze(input)
            analysisResult.diagnostics + runRules(input.database, analysisResult.files, ruleSetProviders, config)
        }

    private fun runRules(
        database: DatabaseContext,
        files: List<SourceFile>,
        ruleSetProviders: List<RuleSetProvider>,
        config: CheckConfig,
    ): List<Diagnostic> {
        val resolver = ConfigurationResolver(config)
        val rules =
            ruleSetProviders.flatMap { provider ->
                provider.ruleProviders().map { ruleProvider ->
                    RuleCandidate(provider.id, ruleProvider.create())
                }
            }
        return files.flatMap { file -> runRulesForFile(database, file, rules, resolver) }
    }

    private fun runRulesForFile(
        database: DatabaseContext,
        file: SourceFile,
        rules: List<RuleCandidate>,
        resolver: ConfigurationResolver,
    ): List<Diagnostic> {
        val facts = SourceSqlFactsExtractor.extract(file)
        val disableDirectives = DisableDirectives.parse(file)
        return rules.flatMap { candidate ->
            val ruleSetConfig = resolver.resolveRuleSet(candidate.ruleSetId, database.name)
            val ruleConfig =
                resolver.resolveRule(
                    ruleId = candidate.rule.id,
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

            val diagnostics = mutableListOf<Diagnostic>()
            candidate.rule.run(
                context,
                DiagnosticReporter { diagnostic ->
                    diagnostics += diagnostic.copy(severity = ruleConfig.severity)
                },
            )
            diagnostics
        }.filterNot(disableDirectives::suppresses)
    }

    private fun Rule.shouldRun(
        context: RuleContext,
        enablement: Enablement,
    ): Boolean =
        when (enablement) {
            Enablement.Enabled -> true
            Enablement.Disabled -> false
            Enablement.Auto -> isApplicable(context)
        }
}

private data class RuleCandidate(
    val ruleSetId: RuleSetId,
    val rule: Rule,
)
