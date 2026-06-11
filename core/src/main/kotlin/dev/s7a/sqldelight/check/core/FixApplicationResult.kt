package dev.s7a.sqldelight.check.core

import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.SourceFile

/**
 * Result of applying diagnostic fixes to one source file.
 */
public data class FixApplicationResult(
    /**
     * Updated file content after all selected fixes were applied.
     */
    public val content: String,
    /**
     * Number of fixes applied.
     */
    public val appliedFixes: Int,
    /**
     * Number of fixes skipped because they were unsafe, invalid, or overlapped a selected fix.
     */
    public val skippedFixes: Int,
    /**
     * Structured details for fixes that were not applied.
     */
    public val skippedFixDetails: List<SkippedFix> = emptyList(),
) {
    public constructor(
        content: String,
        appliedFixes: Int,
        skippedFixes: Int,
    ) : this(
        content = content,
        appliedFixes = appliedFixes,
        skippedFixes = skippedFixes,
        skippedFixDetails = emptyList(),
    )
}

/**
 * A diagnostic fix that was not applied.
 */
public data class SkippedFix(
    /**
     * Rule ID that produced the fix when available.
     */
    public val ruleId: RuleId?,
    /**
     * File that contained the skipped fix when available.
     */
    public val file: SourceFile?,
    /**
     * User-facing fix title.
     */
    public val title: String,
    /**
     * Reason the fix was not applied.
     */
    public val reason: FixSkipReason,
)

/**
 * Structured reason a diagnostic fix was not applied.
 */
public enum class FixSkipReason {
    /**
     * The fix is unsafe and unsafe fixes were not enabled.
     */
    Unsafe,

    /**
     * At least one edit range could not be resolved in the source content.
     */
    InvalidRange,

    /**
     * Edits inside the same fix overlap each other.
     */
    OverlappingEdits,

    /**
     * The fix overlaps another selected diagnostic fix candidate.
     */
    OverlappingCandidate,
}
