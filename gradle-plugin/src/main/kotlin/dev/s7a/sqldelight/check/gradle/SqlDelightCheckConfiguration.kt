package dev.s7a.sqldelight.check.gradle

import dev.s7a.sqldelight.check.api.LogLevel
import dev.s7a.sqldelight.check.api.QualifiedRuleId
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.RuleSetId
import dev.s7a.sqldelight.check.core.Baseline
import dev.s7a.sqldelight.check.core.CheckConfig
import dev.s7a.sqldelight.check.core.DatabaseConfig
import dev.s7a.sqldelight.check.core.RuleConfig
import dev.s7a.sqldelight.check.core.RuleSetConfig

/**
 * Converts the Gradle extension model into the immutable core configuration used during task execution.
 */
internal fun SqlDelightCheckExtension.toCheckConfig(
    logLevel: LogLevel = this.logLevel.get(),
    baseline: Baseline = Baseline.Empty,
): CheckConfig =
    CheckConfig(
        ruleSets = ruleSets.toRuleSetConfigs(),
        rules = rules.toRuleConfigs(),
        databases =
            databases.associate { database ->
                val config =
                    DatabaseConfig(
                        name = database.name,
                        ruleSets = database.ruleSets.toRuleSetConfigs(),
                        rules = database.rules.toRuleConfigs(),
                    )
                database.name to config
        },
        allowUnsafeFixes = fix.unsafe.get(),
        baseline = baseline,
        logLevel = logLevel,
    )

private fun Iterable<RuleSetExtension>.toRuleSetConfigs(): Map<RuleSetId, RuleSetConfig> =
    associate { ruleSet ->
        val id = RuleSetId(ruleSet.name)
        id to RuleSetConfig(id = id, enablement = ruleSet.enabled.orNull)
    }

private fun Iterable<RuleExtension>.toRuleConfigs(): Map<QualifiedRuleId, RuleConfig> =
    associate { rule ->
        val id = rule.name.toQualifiedRuleId()
        id to
            RuleConfig(
                id = id,
                enablement = rule.enabled.orNull,
                severity = rule.severity.get(),
                options = rule.options.get(),
            )
    }

private fun String.toQualifiedRuleId(): QualifiedRuleId {
    val delimiter = indexOf(':')
    require(delimiter in 1..<lastIndex) {
        "Rule ID must use rule-set:rule-name form: $this"
    }
    return QualifiedRuleId(
        ruleSetId = RuleSetId(substring(0, delimiter)),
        ruleId = RuleId(substring(delimiter + 1)),
    )
}
