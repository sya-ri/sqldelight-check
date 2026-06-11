package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.SqlDialectSourceTerm

internal fun String.hasWhereClauseAfter(
    tokens: List<SqlToken>,
    startIndex: Int,
    statementEnd: Int,
    depth: Int,
): Boolean =
    tokens
        .drop(startIndex + 1)
        .takeWhile { candidate -> candidate.startOffset < statementEnd }
        .any { candidate -> candidate.isTerm(SqlDialectSourceTerm.Where) && sqlParenthesisDepthAt(candidate.startOffset) == depth }
