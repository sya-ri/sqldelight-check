package dev.s7a.sqldelight.check.core

import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.RuleSetId

/**
 * Database-specific configuration overrides.
 */
public data class DatabaseConfig(
    /**
     * SQLDelight database name.
     */
    public val name: String,
    /**
     * Rule set overrides for this database.
     */
    public val ruleSets: Map<RuleSetId, RuleSetConfig> = emptyMap(),
    /**
     * Rule overrides for this database.
     */
    public val rules: Map<RuleId, RuleConfig> = emptyMap(),
)
