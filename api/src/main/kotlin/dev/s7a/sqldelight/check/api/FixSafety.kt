package dev.s7a.sqldelight.check.api

/**
 * Safety classification for a write operation proposed by a rule or formatter.
 */
public enum class FixSafety {
    /** The edit is expected to preserve behavior and may run during normal write tasks. */
    Safe,

    /** The edit may change behavior and requires explicit user opt-in. */
    Unsafe,
}
