package dev.s7a.sqldelight.check.api

/**
 * Source range in a file.
 */
public data class SourceRange(
    /** Inclusive start position. */
    public val start: SourcePosition,
    /** Exclusive end position. */
    public val end: SourcePosition,
)
