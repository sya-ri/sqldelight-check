package dev.s7a.sqldelight.check.api

/**
 * A fix proposed by a rule diagnostic.
 */
public data class Fix(
    /**
     * Short title shown in reports or IDE integrations.
     */
    public val title: String,
    /**
     * Whether this fix may be applied by normal write tasks.
     */
    public val safety: FixSafety,
    /**
     * Ordered edits that make up this fix.
     */
    public val edits: List<TextEdit>,
)
