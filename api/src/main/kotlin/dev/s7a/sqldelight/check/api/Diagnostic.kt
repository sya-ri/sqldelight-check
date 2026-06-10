package dev.s7a.sqldelight.check.api

/**
 * Diagnostic emitted by a rule, core analyzer, formatter, or configuration validation.
 */
public data class Diagnostic(
    /**
     * Rule ID responsible for the diagnostic when available.
     */
    public val ruleId: RuleId?,
    /**
     * Resolved severity.
     */
    public val severity: Severity,
    /**
     * User-facing message.
     */
    public val message: String,
    /**
     * File where the diagnostic occurred.
     */
    public val file: SourceFile?,
    /**
     * Source range where the diagnostic occurred.
     */
    public val range: SourceRange?,
    /**
     * Database context associated with the diagnostic when known.
     */
    public val database: DatabaseContext?,
    /**
     * Optional fixes for the diagnostic.
     */
    public val fixes: List<Fix> = emptyList(),
)
