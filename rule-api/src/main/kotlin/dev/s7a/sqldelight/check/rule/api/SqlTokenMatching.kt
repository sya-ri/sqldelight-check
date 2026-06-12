package dev.s7a.sqldelight.check.rule.api

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatterns

/**
 * A matched range in a SQL token list.
 */
public class SqlTokenMatch(
    public val startToken: SqlToken,
    public val endToken: SqlToken,
) {
    override fun equals(other: Any?): Boolean =
        this === other ||
            other is SqlTokenMatch &&
            startToken == other.startToken &&
            endToken == other.endToken

    override fun hashCode(): Int = 31 * startToken.hashCode() + endToken.hashCode()

    override fun toString(): String =
        "SqlTokenMatch(startToken=$startToken, endToken=$endToken)"
}

/**
 * Returns the first source pattern match for [role].
 */
public fun List<SqlToken>.findSourcePattern(
    role: SqlDialectSourcePatternRole,
    sourcePatterns: SqlDialectSourcePatterns,
): SqlTokenMatch? =
    indices
        .asSequence()
        .mapNotNull { index -> sourcePatternAt(index, role, sourcePatterns) }
        .firstOrNull()

/**
 * Returns true when a source pattern match for [role] exists.
 */
public fun List<SqlToken>.containsSourcePattern(
    role: SqlDialectSourcePatternRole,
    sourcePatterns: SqlDialectSourcePatterns,
): Boolean =
    findSourcePattern(role, sourcePatterns) != null

/**
 * Returns the first ordered sequence of source pattern matches.
 *
 * Other tokens may appear between matched source patterns.
 */
public fun List<SqlToken>.findSourcePatternsInOrder(
    sourcePatterns: SqlDialectSourcePatterns,
    vararg roles: SqlDialectSourcePatternRole,
): SqlTokenMatch? {
    var startToken: SqlToken? = null
    var endToken: SqlToken? = null
    var searchIndex = 0
    roles.forEach { role ->
        val match =
            indices
                .asSequence()
                .dropWhile { index -> index < searchIndex }
                .mapNotNull { index -> sourcePatternAt(index, role, sourcePatterns) }
                .firstOrNull()
                ?: return null
        if (startToken == null) startToken = match.startToken
        endToken = match.endToken
        searchIndex = indexOf(match.endToken) + 1
    }
    return SqlTokenMatch(
        startToken = startToken ?: return null,
        endToken = endToken ?: return null,
    )
}

/**
 * Reports a diagnostic for [match] using this rule's default severity.
 */
public fun Rule.reportSqlTokenMatch(
    context: RuleContext,
    reporter: DiagnosticReporter,
    message: String,
    match: SqlTokenMatch,
) {
    reporter.report(
        RuleDiagnostic(
            severity = defaultSeverity,
            message = message,
            file = context.file,
            range = context.file.content.rangeAtOffsets(match.startToken.startOffset, match.endToken.endOffset),
            database = context.database,
        ),
    )
}

/**
 * Tokenizes the current file and reports matches found per statement.
 */
public fun Rule.reportSqlStatementMatches(
    context: RuleContext,
    reporter: DiagnosticReporter,
    message: String,
    hashLineComments: Boolean = false,
    findMatch: (statement: List<SqlToken>) -> SqlTokenMatch?,
) {
    if (!isApplicable(context)) return
    context.file.content
        .sqlTokens(hashLineComments = hashLineComments)
        .toList()
        .sqlStatements()
        .mapNotNull(findMatch)
        .forEach { match ->
            reportSqlTokenMatch(
                context = context,
                reporter = reporter,
                message = message,
                match = match,
            )
        }
}

private fun List<SqlToken>.sourcePatternAt(
    index: Int,
    role: SqlDialectSourcePatternRole,
    sourcePatterns: SqlDialectSourcePatterns,
): SqlTokenMatch? {
    val length = sourcePatterns.matchPrefix(role, drop(index).map { token -> token.normalizedText }) ?: return null
    return SqlTokenMatch(
        startToken = get(index),
        endToken = get(index + length - 1),
    )
}
