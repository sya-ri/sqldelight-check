package dev.s7a.sqldelight.check.core

import dev.s7a.sqldelight.check.api.LogLevel
import dev.s7a.sqldelight.check.api.QualifiedRuleId
import dev.s7a.sqldelight.check.api.RuleSetId

/**
 * Global sqldelight-check configuration.
 */
public class CheckConfig(
    /**
     * Global rule set configuration.
     */
    public val ruleSets: Map<RuleSetId, RuleSetConfig> = emptyMap(),
    /**
     * Global rule configuration.
     */
    public val rules: Map<QualifiedRuleId, RuleConfig> = emptyMap(),
    /**
     * Database-specific configuration.
     */
    public val databases: Map<String, DatabaseConfig> = emptyMap(),
    /**
     * Whether fix tasks may apply unsafe fixes.
     */
    public val allowUnsafeFixes: Boolean = false,
    /**
     * Logging verbosity for task execution.
     */
    public val logLevel: LogLevel = LogLevel.Info,
) {
    override fun equals(other: Any?): Boolean =
        this === other ||
            other is CheckConfig &&
            ruleSets == other.ruleSets &&
            rules == other.rules &&
            databases == other.databases &&
            allowUnsafeFixes == other.allowUnsafeFixes &&
            logLevel == other.logLevel

    override fun hashCode(): Int {
        var result = ruleSets.hashCode()
        result = 31 * result + rules.hashCode()
        result = 31 * result + databases.hashCode()
        result = 31 * result + allowUnsafeFixes.hashCode()
        result = 31 * result + logLevel.hashCode()
        return result
    }

    override fun toString(): String =
        "CheckConfig(ruleSets=$ruleSets, rules=$rules, databases=$databases, allowUnsafeFixes=$allowUnsafeFixes, logLevel=$logLevel)"
}
