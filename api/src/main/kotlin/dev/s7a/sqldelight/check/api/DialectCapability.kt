package dev.s7a.sqldelight.check.api

/**
 * Dialect capability identifier used by rules to check feature support without depending on artifact names.
 */
public data class DialectCapability(
    /** Stable capability ID. */
    public val id: String,
)
