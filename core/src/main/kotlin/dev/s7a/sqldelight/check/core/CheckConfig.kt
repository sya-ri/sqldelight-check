package dev.s7a.sqldelight.check.core

import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.RuleSetId

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
