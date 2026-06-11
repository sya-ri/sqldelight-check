package dev.s7a.sqldelight.check.core

import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.RuleSetId

/**
 * Database-specific configuration overrides.
 */
public class DatabaseConfig(
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
) {
    override fun equals(other: Any?): Boolean =
        this === other ||
            other is DatabaseConfig &&
            name == other.name &&
            ruleSets == other.ruleSets &&
            rules == other.rules

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + ruleSets.hashCode()
        result = 31 * result + rules.hashCode()
        return result
    }

    override fun toString(): String = "DatabaseConfig(name=$name, ruleSets=$ruleSets, rules=$rules)"
}
