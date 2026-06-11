package dev.s7a.sqldelight.check.api

/**
 * Source range in a file.
 */
public class SourceRange(
    /**
     * Inclusive start position.
     */
    public val start: SourcePosition,
    /**
     * Exclusive end position.
     */
    public val end: SourcePosition,
) {
    override fun equals(other: Any?): Boolean =
        this === other ||
            other is SourceRange &&
            start == other.start &&
            end == other.end

    override fun hashCode(): Int = 31 * start.hashCode() + end.hashCode()

    override fun toString(): String = "SourceRange(start=$start, end=$end)"
}
