package dev.s7a.sqldelight.check.core

import dev.s7a.sqldelight.check.adapter.spi.AnalysisInput
import dev.s7a.sqldelight.check.adapter.spi.AnalysisResult
import dev.s7a.sqldelight.check.adapter.spi.SqlDelightAdapter
import dev.s7a.sqldelight.check.api.DatabaseContext
import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.RuleSetId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SourceFile
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext
import dev.s7a.sqldelight.check.rule.api.RuleSetProvider

/**
 * Resolved configuration for one rule after global and database-specific overrides are applied.
 */
public data class ResolvedRuleConfig(
    /** Rule ID being configured. */
    public val ruleId: RuleId,
    /** Final enablement before `Auto` applicability is evaluated. */
    public val enablement: Enablement,
    /** Final severity assigned to diagnostics from this rule. */
    public val severity: Severity,
)

/**
 * Resolved configuration for one rule set.
 */
public data class ResolvedRuleSetConfig(
    /** Rule set ID being configured. */
    public val ruleSetId: RuleSetId,
    /** Final enablement before rule-level overrides are applied. */
    public val enablement: Enablement,
)

/**
 * Entry point for sqldelight-check analysis.
 */
public class SqlDelightCheckEngine {
    /**
     * Runs analysis for all resolved SQLDelight databases.
     *
     * FIXME: Wire this to rule discovery, formatter execution, write validation, and reporter metadata.
     */
    public fun run(
        inputs: List<AnalysisInput> = emptyList(),
        adapter: SqlDelightAdapter? = null,
        ruleSetProviders: List<RuleSetProvider> = emptyList(),
    ): List<Diagnostic> =
        inputs.flatMap { input ->
            val analysisResult =
                adapter
                    ?.analyze(input)
                    ?: AnalysisResult(files = input.files, diagnostics = emptyList())
            analysisResult.diagnostics + runRules(input.database, analysisResult.files, ruleSetProviders)
        }

    private fun runRules(
        database: DatabaseContext,
        files: List<SourceFile>,
        ruleSetProviders: List<RuleSetProvider>,
    ): List<Diagnostic> {
        // FIXME: Apply resolved user configuration and severity overrides before v0.1.0 release.
        val rules = ruleSetProviders.flatMap { provider -> provider.ruleProviders().map { ruleProvider -> ruleProvider.create() } }
        return files.flatMap { file -> runRulesForFile(database, file, rules) }
    }

    private fun runRulesForFile(
        database: DatabaseContext,
        file: SourceFile,
        rules: List<Rule>,
    ): List<Diagnostic> {
        val context =
            object : RuleContext {
                override val database: DatabaseContext = database
                override val file: SourceFile = file
            }
        return rules.flatMap { rule ->
            if (!rule.shouldRun(context)) return@flatMap emptyList()
            val diagnostics = mutableListOf<Diagnostic>()
            rule.run(context, DiagnosticReporter { diagnostic -> diagnostics += diagnostic })
            diagnostics
        }
    }

    private fun Rule.shouldRun(context: RuleContext): Boolean =
        when (defaultEnablement) {
            Enablement.Enabled -> true
            Enablement.Disabled -> false
            Enablement.Auto -> isApplicable(context)
        }
}
