package dev.s7a.sqldelight.check.core

import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.RuleSetId
import dev.s7a.sqldelight.check.api.Severity

/**
 * Resolves global and database-specific configuration into final per-database values.
 */
public class ConfigurationResolver(
    private val config: CheckConfig,
) {
    /**
     * Resolves a rule set for a database.
     */
    public fun resolveRuleSet(
        ruleSetId: RuleSetId,
        databaseName: String,
        defaultEnablement: Enablement = Enablement.Auto,
    ): ResolvedRuleSetConfig {
        val global = config.ruleSets[ruleSetId]
        val database = config.databases[databaseName]?.ruleSets?.get(ruleSetId)
        return ResolvedRuleSetConfig(
            ruleSetId = ruleSetId,
            enablement = database?.enablement ?: global?.enablement ?: defaultEnablement,
        )
    }

    /**
     * Resolves a rule for a database.
     */
    public fun resolveRule(
        ruleId: RuleId,
        databaseName: String,
        defaultEnablement: Enablement = Enablement.Auto,
        defaultSeverity: Severity = Severity.Warning,
    ): ResolvedRuleConfig {
        val global = config.rules[ruleId]
        val database = config.databases[databaseName]?.rules?.get(ruleId)
        return ResolvedRuleConfig(
            ruleId = ruleId,
            enablement = database?.enablement ?: global?.enablement ?: defaultEnablement,
            severity = database?.severity ?: global?.severity ?: defaultSeverity,
        )
    }
}
