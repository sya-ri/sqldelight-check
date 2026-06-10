package dev.s7a.sqldelight.check.api

/**
 * Identifies a rule set such as `standard`, `sqlite`, or a third-party provider ID.
 */
@JvmInline
public value class RuleSetId(
    public val value: String,
)
