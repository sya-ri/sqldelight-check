package dev.s7a.sqldelight.check.gradle

import dev.s7a.sqldelight.check.api.LogLevel
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.RuleSetId
import dev.s7a.sqldelight.check.core.CheckConfig
import dev.s7a.sqldelight.check.core.DatabaseConfig
import dev.s7a.sqldelight.check.core.RuleConfig
import dev.s7a.sqldelight.check.core.RuleSetConfig

/**
 * Converts the Gradle extension model into the immutable core configuration used during task execution.
 */
internal fun SqlDelightCheckExtension.toCheckConfig(logLevel: LogLevel = this.logLevel.get()): CheckConfig =
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
        allowUnsafeWrites = write.unsafe.get(),
        logLevel = logLevel,
    )

private fun Iterable<RuleSetExtension>.toRuleSetConfigs(): Map<RuleSetId, RuleSetConfig> =
    associate { ruleSet ->
        val id = RuleSetId(ruleSet.name)
        id to RuleSetConfig(id = id, enablement = ruleSet.enabled.get())
    }

private fun Iterable<RuleExtension>.toRuleConfigs(): Map<RuleId, RuleConfig> =
    associate { rule ->
        val id = RuleId(rule.name)
        id to
            RuleConfig(
                id = id,
                enablement = rule.enabled.get(),
                severity = rule.severity.get(),
                options = rule.options.get(),
            )
    }
