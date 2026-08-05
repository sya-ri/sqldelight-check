package dev.s7a.sqldelight.check.api

/**
 * Dialect-aware source structure for conservative SQL source scanners.
 *
 * The structure keeps source tokens together with statement, parenthesis,
 * `CASE` nesting, and source-pattern matches. It is intentionally lighter than
 * a parser grammar; SQLDelight remains responsible for validating SQL.
 */
public class SqlSourceStructure(
    public val tokens: List<SqlSourceTokenContext>,
    public val blocks: List<SqlSourceBlock>,
) {
    private val tokensByStatement: Map<Int, List<SqlSourceTokenContext>> =
        tokens.groupBy { it.statementIndex }

    /**
     * Returns tokens that belong to the statement with [statementIndex].
     */
    public fun tokensInStatement(statementIndex: Int): List<SqlSourceTokenContext> =
        tokensByStatement[statementIndex] ?: emptyList()

    /**
     * Returns the token context that covers [offset], if any.
     */
    public fun contextAtOffset(offset: Int): SqlSourceTokenContext? {
        var lo = 0
        var hi = tokens.size - 1
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            val ctx = tokens[mid]
            when {
                offset < ctx.token.startOffset -> hi = mid - 1
                offset >= ctx.token.endOffset -> lo = mid + 1
                else -> return ctx
            }
        }
        return null
    }

    /**
     * Returns the smallest block that contains [token].
     */
    public fun innermostBlockContaining(token: SqlSourceTokenContext): SqlSourceBlock? {
        var result: SqlSourceBlock? = null
        blocks.forEach { block ->
            val current = result
            if (block.contains(token) && (current == null || block.size < current.size)) {
                result = block
            }
        }
        return result
    }

    /**
     * Returns the tokens contained by [block].
     */
    public fun tokensInBlock(block: SqlSourceBlock): List<SqlSourceTokenContext> =
        tokens.subList(block.startTokenIndex, block.endTokenIndex)

    /**
     * Returns blocks whose direct parent is [block].
     */
    public fun childBlocks(block: SqlSourceBlock): List<SqlSourceBlock> {
        val parentIndex = blocks.indexOf(block)
        if (parentIndex == -1) return emptyList()

        val children = mutableListOf<SqlSourceBlock>()
        blocks.forEach { child ->
            if (child.parentBlockIndex == parentIndex) {
                children += child
            }
        }
        return children
    }

    override fun equals(other: Any?): Boolean =
        this === other ||
            other is SqlSourceStructure &&
            tokens == other.tokens &&
            blocks == other.blocks

    override fun hashCode(): Int = 31 * tokens.hashCode() + blocks.hashCode()

    override fun toString(): String =
        "SqlSourceStructure(tokens=$tokens, blocks=$blocks)"

    public companion object {
        /**
         * Parses [source] into a dialect-aware source structure.
         */
        public fun parse(
            source: String,
            sourcePatterns: SqlDialectSourcePatterns = SqlDialectSourcePatterns.SourceScannerDefault,
        ): SqlSourceStructure =
            SqlSourceStructureParser.parse(source, sourcePatterns)
    }
}
