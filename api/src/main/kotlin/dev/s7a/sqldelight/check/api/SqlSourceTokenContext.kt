package dev.s7a.sqldelight.check.api

/**
 * A SQL source token with its surrounding source structure.
 *
 * Depth values describe the state before [token] is consumed. For example, an
 * opening parenthesis at top level has `parenthesisDepth == 0`, and tokens
 * inside that parenthesis have `parenthesisDepth == 1`.
 */
public class SqlSourceTokenContext(
    public val token: SqlSourceToken,
    public val index: Int,
    public val statementIndex: Int,
    public val parenthesisDepth: Int,
    public val caseDepth: Int,
    public val patternMatches: Set<SqlSourcePatternMatch>,
) {
    /**
     * Returns true when a dialect pattern with [role] starts at this token.
     */
    public fun matches(role: SqlDialectSourcePatternRole): Boolean =
        patternMatches.any { match -> role in match.roles }

    /**
     * Returns the longest matched pattern length for [role], if any.
     */
    public fun matchLength(role: SqlDialectSourcePatternRole): Int? {
        var result: Int? = null
        patternMatches.forEach { match ->
            val current = result
            if (role in match.roles && (current == null || match.length > current)) {
                result = match.length
            }
        }
        return result
    }

    override fun equals(other: Any?): Boolean =
        this === other ||
            other is SqlSourceTokenContext &&
            token == other.token &&
            index == other.index &&
            statementIndex == other.statementIndex &&
            parenthesisDepth == other.parenthesisDepth &&
            caseDepth == other.caseDepth &&
            patternMatches == other.patternMatches

    override fun hashCode(): Int {
        var result = token.hashCode()
        result = 31 * result + index
        result = 31 * result + statementIndex
        result = 31 * result + parenthesisDepth
        result = 31 * result + caseDepth
        result = 31 * result + patternMatches.hashCode()
        return result
    }

    override fun toString(): String =
        "SqlSourceTokenContext(token=$token, index=$index, statementIndex=$statementIndex, " +
            "parenthesisDepth=$parenthesisDepth, caseDepth=$caseDepth, patternMatches=$patternMatches)"
}
