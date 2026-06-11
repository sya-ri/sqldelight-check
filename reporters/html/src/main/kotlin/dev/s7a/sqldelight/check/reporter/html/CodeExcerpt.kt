package dev.s7a.sqldelight.check.reporter.html

/**
 * Source excerpt rendered in an HTML diagnostic card.
 */
internal data class CodeExcerpt(
    val lines: List<CodeExcerptLine>,
)
