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
) {
    /**
     * Returns tokens that belong to the statement with [statementIndex].
     */
    public fun tokensInStatement(statementIndex: Int): List<SqlSourceTokenContext> =
        tokens.filter { context -> context.statementIndex == statementIndex }

    /**
     * Returns the token context that covers [offset], if any.
     */
    public fun contextAtOffset(offset: Int): SqlSourceTokenContext? =
        tokens.firstOrNull { context -> offset in context.token.startOffset..<context.token.endOffset }

    override fun equals(other: Any?): Boolean =
        this === other ||
            other is SqlSourceStructure &&
            tokens == other.tokens

    override fun hashCode(): Int = tokens.hashCode()

    override fun toString(): String =
        "SqlSourceStructure(tokens=$tokens)"

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
