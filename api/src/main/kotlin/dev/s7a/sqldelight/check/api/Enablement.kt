package dev.s7a.sqldelight.check.api

/**
 * User-facing enablement state for rules and rule sets.
 */
public enum class Enablement {
    /**
     * Let sqldelight-check decide from database, dialect, SQLDelight version, and rule applicability.
     */
    Auto,

    /**
     * Explicitly enable the rule or rule set.
     */
    Enabled,

    /**
     * Explicitly disable the rule or rule set.
     */
    Disabled,
}
