package dev.s7a.sqldelight.check.api

/**
 * Text edit represented as a replacement over a source range.
 */
public data class TextEdit(
    /**
     * Range to replace.
     */
    public val range: SourceRange,
    /**
     * Replacement text.
     */
    public val replacement: String,
)
