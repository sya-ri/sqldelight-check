package dev.s7a.sqldelight.check.core

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
