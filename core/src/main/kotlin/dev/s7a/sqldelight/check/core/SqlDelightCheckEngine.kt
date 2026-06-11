@file:OptIn(InternalSqldelightCheckApi::class)

package dev.s7a.sqldelight.check.core

import dev.s7a.sqldelight.check.api.DatabaseContext
import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.InternalSqldelightCheckApi
import dev.s7a.sqldelight.check.api.QualifiedRuleId
import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleSetId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SourceFile
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext
import dev.s7a.sqldelight.check.rule.api.RuleSetProvider
import dev.s7a.sqldelight.check.rule.api.SqlFacts
import dev.s7a.sqldelight.check.rule.api.defaultEnablement

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
        return files.flatMap { file -> runRulesForFile(database, file, rules, resolver, trace) }
    }

    private fun runRulesForFile(
        database: DatabaseContext,
        file: SourceFile,
        rules: List<RuleCandidate>,
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
                val enablement =
                    if (ruleConfig.enablement == Enablement.Auto) {
                        ruleSetConfig.enablement
                    } else {
                        ruleConfig.enablement
                    }
                if (!candidate.rule.shouldRun(context, enablement)) return@flatMap emptyList()

                executedRuleIds += candidate.ruleId
                val diagnostics = mutableListOf<Diagnostic>()
                candidate.rule.run(context) { diagnostic ->
                    diagnostics += diagnostic.withRuleIdentity(candidate.ruleId, ruleConfig.severity)
                }
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

private data class RuleCandidate(
    val ruleSetId: RuleSetId,
    val ruleId: QualifiedRuleId,
    val rule: Rule,
)
