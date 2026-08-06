package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.SqlDialectSourceTerm
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext
import dev.s7a.sqldelight.check.rule.api.SqlToken as SourceSqlToken
import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets
import dev.s7a.sqldelight.check.rule.api.sqlTokens as sourceSqlTokens

internal fun Rule.reportAlwaysTrueWhere(
    reporter: DiagnosticReporter,
    context: RuleContext,
    statementTerm: SqlDialectSourceTerm,
    message: String,
) {
    val content = context.file.content
    val tokens = content.sourceSqlTokens().toList()
    val parenthesisDepths = content.computeParenthesisDepths()
    tokens.forEachIndexed { index, token ->
        if (!token.matches(statementTerm)) return@forEachIndexed
        val depth = parenthesisDepths[token.startOffset]
        val statementEnd = content.statementEndAfter(token.startOffset)
        val statementTokens =
            tokens
                .drop(index + 1)
                .takeWhile { candidate -> candidate.startOffset < statementEnd }
                .filter { candidate -> parenthesisDepths[candidate.startOffset] == depth }
        val where = statementTokens.firstOrNull { candidate -> candidate.matches(SqlDialectSourceTerm.Where) } ?: return@forEachIndexed
        if (!content.hasAlwaysTrueConditionAfter(statementTokens, where, statementEnd)) return@forEachIndexed

        reporter.report(
            RuleDiagnostic(
                severity = defaultSeverity,
                message = message,
                file = context.file,
                range = content.rangeAtOffsets(where.startOffset, where.endOffset),
                database = context.database,
            ),
        )
    }
}

private fun String.hasAlwaysTrueConditionAfter(
    statementTokens: List<SourceSqlToken>,
    where: SourceSqlToken,
    statementEnd: Int,
): Boolean {
    val conditionTokens = statementTokens.dropWhile { token -> token.startOffset <= where.startOffset }
    val first = conditionTokens.firstOrNull() ?: return false
    if (first.matches(SqlDialectSourceTerm.True)) return true

    val second = conditionTokens.getOrNull(1) ?: return false
    if (first.text != "1" || second.text != "1") return false
    val between = substring(first.endOffset, second.startOffset)
    val afterSecond = substring(second.endOffset, statementEnd)
    return between.all { character -> character.isWhitespace() || character == '=' } &&
        between.any { character -> character == '=' } &&
        afterSecond.all { character -> character.isWhitespace() || character == ';' }
}

internal fun SourceSqlToken.matches(term: SqlDialectSourceTerm): Boolean = normalizedText == term.normalizedText
