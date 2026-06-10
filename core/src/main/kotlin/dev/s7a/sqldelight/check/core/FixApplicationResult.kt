package dev.s7a.sqldelight.check.core

/**
 * Result of applying diagnostic fixes to one source file.
 */
public data class FixApplicationResult(
    /** Updated file content after all selected fixes were applied. */
    public val content: String,
    /** Number of fixes applied. */
    public val appliedFixes: Int,
    /** Number of fixes skipped because they were unsafe, invalid, or overlapped a selected fix. */
    public val skippedFixes: Int,
)
