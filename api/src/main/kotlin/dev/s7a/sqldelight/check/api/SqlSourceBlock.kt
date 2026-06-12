package dev.s7a.sqldelight.check.api

/**
 * A syntactic source block derived from dialect-aware source tokens.
 *
 * Block ranges use token indices, with [endTokenIndex] being exclusive. The
 * source offsets cover the same range in the original source text.
 */
public class SqlSourceBlock(
    public val kind: SqlSourceBlockKind,
    public val startTokenIndex: Int,
    public val endTokenIndex: Int,
    public val startOffset: Int,
    public val endOffset: Int,
    public val statementIndex: Int,
    public val parentBlockIndex: Int?,
    public val sourcePatternMatch: SqlSourcePatternMatch? = null,
) {
    /**
     * Number of tokens covered by this block.
     */
    public val size: Int
        get() = endTokenIndex - startTokenIndex

    /**
     * Returns true when [token] is inside this block.
     */
    public fun contains(token: SqlSourceTokenContext): Boolean =
        token.index in startTokenIndex..<endTokenIndex

    /**
     * Returns true when [block] is fully inside this block.
     */
    public fun contains(block: SqlSourceBlock): Boolean =
        startTokenIndex <= block.startTokenIndex && block.endTokenIndex <= endTokenIndex

    override fun equals(other: Any?): Boolean =
        this === other ||
            other is SqlSourceBlock &&
            kind == other.kind &&
            startTokenIndex == other.startTokenIndex &&
            endTokenIndex == other.endTokenIndex &&
            startOffset == other.startOffset &&
            endOffset == other.endOffset &&
            statementIndex == other.statementIndex &&
            parentBlockIndex == other.parentBlockIndex &&
            sourcePatternMatch == other.sourcePatternMatch

    override fun hashCode(): Int {
        var result = kind.hashCode()
        result = 31 * result + startTokenIndex
        result = 31 * result + endTokenIndex
        result = 31 * result + startOffset
        result = 31 * result + endOffset
        result = 31 * result + statementIndex
        result = 31 * result + (parentBlockIndex ?: 0)
        result = 31 * result + (sourcePatternMatch?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String =
        "SqlSourceBlock(kind=$kind, startTokenIndex=$startTokenIndex, endTokenIndex=$endTokenIndex, " +
            "startOffset=$startOffset, endOffset=$endOffset, statementIndex=$statementIndex, " +
            "parentBlockIndex=$parentBlockIndex, sourcePatternMatch=$sourcePatternMatch)"
}
