package dev.s7a.sqldelight.check.core

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
