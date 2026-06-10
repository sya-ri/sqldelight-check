package dev.s7a.sqldelight.check.api

/**
 * One-based source position for diagnostics and edits.
 */
public data class SourcePosition(
    /** One-based line number. */
    public val line: Int,
    /** One-based column number. */
    public val column: Int,
)
