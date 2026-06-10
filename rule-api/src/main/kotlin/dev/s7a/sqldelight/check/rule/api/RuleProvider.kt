package dev.s7a.sqldelight.check.rule.api

/**
 * Creates fresh rule instances for thread-safe analysis.
 */
public fun interface RuleProvider {
    /** Creates a new rule instance. */
    public fun create(): Rule
}
