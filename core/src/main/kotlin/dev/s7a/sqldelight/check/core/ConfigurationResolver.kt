package dev.s7a.sqldelight.check.core

import dev.s7a.sqldelight.check.api.QualifiedRuleId
import dev.s7a.sqldelight.check.api.RuleSetId
import dev.s7a.sqldelight.check.api.Severity

/**
 * Resolves global and database-specific configuration into final per-database values.
 */
internal class ConfigurationResolver(
    private val config: CheckConfig,
) {
    /**
     * Resolves a rule set for a database.
     */
    fun resolveRuleSet(
        ruleSetId: RuleSetId,
        databaseName: String,
        defaultEnabled: Boolean? = null,
    ): ResolvedRuleSetConfig {
        val global = config.ruleSets[ruleSetId]
        val database = config.databases[databaseName]?.ruleSets?.get(ruleSetId)
        return ResolvedRuleSetConfig(
            ruleSetId = ruleSetId,
            enablement =
                when {
                    database != null -> database.enablement
                    global != null -> global.enablement
                    else -> defaultEnabled
                },
        )
    }

    /**
     * Resolves a rule for a database.
     */
    fun resolveRule(
        ruleId: QualifiedRuleId,
        databaseName: String,
        defaultEnabled: Boolean? = null,
        defaultSeverity: Severity = Severity.Warning,
    ): ResolvedRuleConfig {
        val global = config.rules[ruleId]
        val database = config.databases[databaseName]?.rules?.get(ruleId)
        return ResolvedRuleConfig(
            ruleId = ruleId,
            enablement =
                when {
                    database != null -> database.enablement
                    global != null -> global.enablement
                    else -> defaultEnabled
                },
            severity = database?.severity ?: global?.severity ?: defaultSeverity,
            options = global.optionsOrEmpty() + database.optionsOrEmpty(),
            explicitlyConfigured = global != null || database != null,
        )
    }

    private fun RuleConfig?.optionsOrEmpty(): Map<String, String> = this?.options ?: emptyMap()
}
