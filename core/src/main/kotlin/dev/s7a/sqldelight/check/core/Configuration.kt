package dev.s7a.sqldelight.check.core

import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.RuleSetId
import dev.s7a.sqldelight.check.api.Severity

/**
 * Rule set configuration before database-specific overrides are applied.
 */
public data class RuleSetConfig(
    /** Rule set ID. */
    public val id: RuleSetId,
    /** Configured rule set enablement. */
    public val enablement: Enablement,
)

/**
 * Rule configuration before database-specific overrides are applied.
 */
public data class RuleConfig(
    /** Rule ID. */
    public val id: RuleId,
    /** Configured rule enablement. */
    public val enablement: Enablement,
    /** Configured rule severity. */
    public val severity: Severity,
)

/**
 * Database-specific configuration overrides.
 */
public data class DatabaseConfig(
    /** SQLDelight database name. */
    public val name: String,
    /** Rule set overrides for this database. */
    public val ruleSets: Map<RuleSetId, RuleSetConfig> = emptyMap(),
    /** Rule overrides for this database. */
    public val rules: Map<RuleId, RuleConfig> = emptyMap(),
)

/**
 * Global sqldelight-check configuration.
 */
public data class CheckConfig(
    /** Global rule set configuration. */
    public val ruleSets: Map<RuleSetId, RuleSetConfig> = emptyMap(),
    /** Global rule configuration. */
    public val rules: Map<RuleId, RuleConfig> = emptyMap(),
    /** Database-specific configuration. */
    public val databases: Map<String, DatabaseConfig> = emptyMap(),
    /** Whether write tasks may apply unsafe fixes. */
    public val allowUnsafeWrites: Boolean = false,
)

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

/**
 * Utility for merging rule set defaults with explicit rule-level overrides.
 */
public object EnablementResolver {
    /**
     * Resolves final rule enablement before `Auto` applicability is evaluated.
     */
    public fun resolveRuleEnablement(
        ruleSetEnablement: Enablement,
        ruleEnablement: Enablement,
    ): Enablement =
        if (ruleEnablement != Enablement.Auto) {
            ruleEnablement
        } else {
            ruleSetEnablement
        }
}

