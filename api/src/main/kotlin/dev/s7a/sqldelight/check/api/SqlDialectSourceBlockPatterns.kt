package dev.s7a.sqldelight.check.api

/**
 * Dialect-specific patterns used to derive conservative SQL source blocks.
 *
 * These patterns are not a validation grammar. They describe source shapes
 * that scanners can use to classify blocks without hard-coding engine syntax.
 */
public class SqlDialectSourceBlockPatterns(
    statementSeparatorTerms: Set<String> = defaultSqlDialectStatementSeparatorTerms,
    public val parenthesisDepthTerms: Set<SqlDialectSourceParenthesisDepthTerms> = defaultSqlDialectParenthesisDepthTerms,
    public val clauseStartRoles: Set<SqlDialectSourcePatternRole> = defaultSqlDialectClauseStartRoles,
    public val pairedBlocks: Set<SqlDialectSourcePairedBlockPattern> = defaultSqlDialectPairedBlocks,
    public val parenthesizedBlocks: Set<SqlDialectSourceParenthesizedBlockPattern> = defaultSqlDialectParenthesizedBlocks,
) {
    public val statementSeparatorTerms: Set<String> = statementSeparatorTerms.normalizedSqlTerms()

    override fun equals(other: Any?): Boolean =
        this === other ||
            other is SqlDialectSourceBlockPatterns &&
            statementSeparatorTerms == other.statementSeparatorTerms &&
            parenthesisDepthTerms == other.parenthesisDepthTerms &&
            clauseStartRoles == other.clauseStartRoles &&
            pairedBlocks == other.pairedBlocks &&
            parenthesizedBlocks == other.parenthesizedBlocks

    override fun hashCode(): Int {
        var result = statementSeparatorTerms.hashCode()
        result = 31 * result + parenthesisDepthTerms.hashCode()
        result = 31 * result + clauseStartRoles.hashCode()
        result = 31 * result + pairedBlocks.hashCode()
        result = 31 * result + parenthesizedBlocks.hashCode()
        return result
    }

    override fun toString(): String =
        "SqlDialectSourceBlockPatterns(statementSeparatorTerms=$statementSeparatorTerms, " +
            "parenthesisDepthTerms=$parenthesisDepthTerms, clauseStartRoles=$clauseStartRoles, " +
            "pairedBlocks=$pairedBlocks, parenthesizedBlocks=$parenthesizedBlocks)"

    public companion object {
        /**
         * Default source block patterns for broad SQL-family scanning.
         */
        public val Default: SqlDialectSourceBlockPatterns =
            SqlDialectSourceBlockPatterns()
    }
}

private val defaultSqlDialectStatementSeparatorTerms: Set<String> =
    setOf(";")

private val defaultSqlDialectParenthesisDepthTerms: Set<SqlDialectSourceParenthesisDepthTerms> =
    setOf(
        SqlDialectSourceParenthesisDepthTerms(
            openTerm = "(",
            closeTerm = ")",
        ),
    )

private val defaultSqlDialectClauseStartRoles: Set<SqlDialectSourcePatternRole> =
    setOf(
        SqlDialectSourcePatternRole.ClauseBoundary,
        SqlDialectSourcePatternRole.SelectListStart,
    )

private val defaultSqlDialectPairedBlocks: Set<SqlDialectSourcePairedBlockPattern> =
    setOf(
        SqlDialectSourcePairedBlockPattern.parse(
            startExpression = "CASE",
            endExpression = "END",
            kind = SqlSourceBlockKind.CaseExpression,
        ),
    )

private val defaultSqlDialectParenthesizedBlocks: Set<SqlDialectSourceParenthesizedBlockPattern> =
    setOf(
        SqlDialectSourceParenthesizedBlockPattern(
            openTerm = "(",
            closeTerm = ")",
            defaultKind = SqlSourceBlockKind.ParenthesizedExpression,
            innerStartRoles =
                setOf(
                    SqlDialectSourcePatternRole.StatementStart,
                    SqlDialectSourcePatternRole.SqlDelightStatementStart,
                    SqlDialectSourcePatternRole.SqlDelightExecutableStatementStart,
                    SqlDialectSourcePatternRole.SelectListStart,
                ),
            innerStartKind = SqlSourceBlockKind.Subquery,
        ),
    )
