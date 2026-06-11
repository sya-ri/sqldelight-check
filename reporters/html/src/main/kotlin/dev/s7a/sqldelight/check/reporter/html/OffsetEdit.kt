package dev.s7a.sqldelight.check.reporter.html

/**
 * Text edit converted to zero-based offsets in source content.
 */
internal data class OffsetEdit(
    val start: Int,
    val end: Int,
    val replacement: String,
)
