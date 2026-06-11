package dev.s7a.sqldelight.check.core

import dev.s7a.sqldelight.check.api.QualifiedRuleId
import dev.s7a.sqldelight.check.api.SourceFile

/**
 * Result of applying diagnostic fixes to one source file.
 */
public class FixApplicationResult(
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

    override fun equals(other: Any?): Boolean =
        this === other ||
            other is FixApplicationResult &&
            content == other.content &&
            appliedFixes == other.appliedFixes &&
            skippedFixes == other.skippedFixes &&
            skippedFixDetails == other.skippedFixDetails

    override fun hashCode(): Int {
        var result = content.hashCode()
        result = 31 * result + appliedFixes
        result = 31 * result + skippedFixes
        result = 31 * result + skippedFixDetails.hashCode()
        return result
    }

    override fun toString(): String =
        "FixApplicationResult(content=$content, appliedFixes=$appliedFixes, skippedFixes=$skippedFixes, skippedFixDetails=$skippedFixDetails)"
}

/**
 * A diagnostic fix that was not applied.
 */
public class SkippedFix(
    /**
     * Rule ID that produced the fix.
     */
    public val ruleId: QualifiedRuleId,
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
) {
    override fun equals(other: Any?): Boolean =
        this === other ||
            other is SkippedFix &&
            ruleId == other.ruleId &&
            file == other.file &&
            title == other.title &&
            reason == other.reason

    override fun hashCode(): Int {
        var result = ruleId.hashCode()
        result = 31 * result + (file?.hashCode() ?: 0)
        result = 31 * result + title.hashCode()
        result = 31 * result + reason.hashCode()
        return result
    }

    override fun toString(): String = "SkippedFix(ruleId=$ruleId, file=$file, title=$title, reason=$reason)"
}

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
