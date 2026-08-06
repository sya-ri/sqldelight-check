package dev.s7a.sqldelight.check.api

/**
 * A source token used by conservative SQL source scanners.
 *
 * Comments, string literals, and quoted identifiers are skipped before tokens
 * are emitted, so punctuation inside them does not affect source nesting.
 */
public class SqlSourceToken(
    public val text: String,
    public val startOffset: Int,
    public val endOffset: Int,
) {
    /**
     * Lowercase token text used for dialect pattern matching.
     */
    public val normalizedText: String = text.lowercase()

    override fun equals(other: Any?): Boolean =
        this === other ||
            other is SqlSourceToken &&
            text == other.text &&
            startOffset == other.startOffset &&
            endOffset == other.endOffset

    override fun hashCode(): Int {
        var result = text.hashCode()
        result = 31 * result + startOffset
        result = 31 * result + endOffset
        return result
    }

    override fun toString(): String =
        "SqlSourceToken(text=$text, startOffset=$startOffset, endOffset=$endOffset)"
}
