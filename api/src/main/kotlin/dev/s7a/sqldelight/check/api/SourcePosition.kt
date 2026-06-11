package dev.s7a.sqldelight.check.api

/**
 * One-based source position for diagnostics and edits.
 */
public class SourcePosition(
    /**
     * One-based line number.
     */
    public val line: Int,
    /**
     * One-based column number.
     */
    public val column: Int,
) {
    override fun equals(other: Any?): Boolean =
        this === other ||
            other is SourcePosition &&
            line == other.line &&
            column == other.column

    override fun hashCode(): Int = 31 * line + column

    override fun toString(): String = "SourcePosition(line=$line, column=$column)"
}
