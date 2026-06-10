package dev.s7a.sqldelight.check.api

/**
 * Identifies a rule using the `rule-set:rule-name` form.
 */
@JvmInline
public value class RuleId(
    public val value: String,
)
