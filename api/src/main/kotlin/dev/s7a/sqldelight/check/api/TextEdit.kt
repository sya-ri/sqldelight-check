package dev.s7a.sqldelight.check.api

/**
 * Text edit represented as a replacement over a source range.
 */
public class TextEdit(
    /**
     * Range to replace.
     */
    public val range: SourceRange,
    /**
     * Replacement text.
     */
    public val replacement: String,
) {
    override fun equals(other: Any?): Boolean =
        this === other ||
            other is TextEdit &&
            range == other.range &&
            replacement == other.replacement

    override fun hashCode(): Int = 31 * range.hashCode() + replacement.hashCode()

    override fun toString(): String = "TextEdit(range=$range, replacement=$replacement)"
}
