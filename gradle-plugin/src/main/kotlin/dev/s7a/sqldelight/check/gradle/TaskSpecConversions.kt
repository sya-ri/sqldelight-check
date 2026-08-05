package dev.s7a.sqldelight.check.gradle

import dev.s7a.sqldelight.check.api.LogLevel
import dev.s7a.sqldelight.check.api.QualifiedRuleId
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.RuleSetId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.core.Baseline
import dev.s7a.sqldelight.check.core.CheckConfig
import dev.s7a.sqldelight.check.core.DatabaseConfig
import dev.s7a.sqldelight.check.core.RuleConfig
import dev.s7a.sqldelight.check.core.RuleSetConfig

/**
 * Reconstructs a [CheckConfig] from task property specs captured at configuration time.
 * Called at task execution time with no project access.
 */
internal fun buildCheckConfig(
    globalRuleSets: List<RuleSetConfigSpec>,
    globalRules: List<RuleConfigSpec>,
    databaseConfigs: List<DatabaseConfigSpec>,
    allowUnsafeFixes: Boolean,
    logLevel: LogLevel,
    baseline: Baseline,
): CheckConfig =
    CheckConfig(
        ruleSets = globalRuleSets.toRuleSetConfigs(),
        rules = globalRules.toRuleConfigs(),
        databases = databaseConfigs.toDatabaseConfigs(),
        allowUnsafeFixes = allowUnsafeFixes,
        logLevel = logLevel,
        baseline = baseline,
    )

private fun List<RuleSetConfigSpec>.toRuleSetConfigs(): Map<RuleSetId, RuleSetConfig> =
    associate { spec ->
        val id = RuleSetId(spec.id.get())
        id to RuleSetConfig(id = id, enablement = spec.enabled.orNull)
    }

private fun List<RuleConfigSpec>.toRuleConfigs(): Map<QualifiedRuleId, RuleConfig> =
    associate { spec ->
        val id = spec.id.get().toQualifiedRuleId()
        id to
            RuleConfig(
                id = id,
                enablement = spec.enabled.orNull,
                severity = Severity.valueOf(spec.severity.get()),
                options = spec.options.get(),
            )
    }

private fun List<DatabaseConfigSpec>.toDatabaseConfigs(): Map<String, DatabaseConfig> =
    associate { spec ->
        val name = spec.name.get()
        name to
            DatabaseConfig(
                name = name,
                ruleSets = spec.ruleSets.get().toRuleSetConfigs(),
                rules = spec.rules.get().toRuleConfigs(),
            )
    }

private fun String.toQualifiedRuleId(): QualifiedRuleId {
    val delimiter = indexOf(':')
    require(delimiter in 1..<lastIndex) { "Rule ID must use rule-set:rule-name form: $this" }
    return QualifiedRuleId(
        ruleSetId = RuleSetId(substring(0, delimiter)),
        ruleId = RuleId(substring(delimiter + 1)),
    )
}
