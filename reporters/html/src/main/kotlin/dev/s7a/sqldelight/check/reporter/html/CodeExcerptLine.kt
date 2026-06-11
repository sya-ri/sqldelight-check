package dev.s7a.sqldelight.check.reporter.html

/**
 * One source line in an HTML code excerpt.
 */
internal data class CodeExcerptLine(
    val number: Int,
    val text: String,
    val highlightStartColumn: Int?,
    val highlightEndColumn: Int?,
)
