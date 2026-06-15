package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SourceFileKind
import dev.s7a.sqldelight.check.api.SqlDialectSourceTerm
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext
import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets
import dev.s7a.sqldelight.check.rule.api.SqlToken as SourceSqlToken
import dev.s7a.sqldelight.check.rule.api.sqlTokens as sourceSqlTokens

/**
 * Reports SQLDelight query SELECTs that can read unbounded result sets.
 */
public class NoUnboundedSelectRule : Rule {
    override val id: RuleId = RuleId("no-unbounded-select")
    override val defaultSeverity: Severity = Severity.Info
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        if (context.file.kind != SourceFileKind.Query) return

        val content = context.file.content
        val tokens = content.sourceSqlTokens().toList()
        tokens.forEachIndexed { index, token ->
            if (!token.matches(SqlDialectSourceTerm.Select)) return@forEachIndexed
            if (content.sqlParenthesisDepthAt(token.startOffset) != 0) return@forEachIndexed
            if (!content.hasQueryLabelBefore(token.startOffset)) return@forEachIndexed

            val statementEnd = content.statementEndAfter(token.startOffset)
            val statementTokens =
                tokens
                    .drop(index + 1)
                    .takeWhile { candidate -> candidate.startOffset < statementEnd }
                    .filter { candidate -> content.sqlParenthesisDepthAt(candidate.startOffset) == 0 }
            if (statementTokens.any { candidate -> candidate.matches(SqlDialectSourceTerm.Where) }) return@forEachIndexed
            if (statementTokens.any { candidate -> candidate.matches(SqlDialectSourceTerm.Limit) }) return@forEachIndexed
            if (content.isAggregateOnlySelect(token, statementTokens, statementEnd)) return@forEachIndexed

            reporter.report(
                RuleDiagnostic(
                    severity = defaultSeverity,
                    message = "SELECT queries should include a WHERE or LIMIT clause unless an unbounded result set is intentional.",
                    file = context.file,
                    range = content.rangeAtOffsets(token.startOffset, token.endOffset),
                    database = context.database,
                ),
            )
        }
    }
}

private fun String.hasQueryLabelBefore(selectOffset: Int): Boolean {
    val lineStart = lastIndexOf('\n', startIndex = selectOffset).let { index -> if (index == -1) 0 else index + 1 }
    val previousLineEnd = (lineStart - 2).coerceAtLeast(0)
    val previousLineStart = lastIndexOf('\n', startIndex = previousLineEnd).let { index -> if (index == -1) 0 else index + 1 }
    val previousLine = substring(previousLineStart, lineStart).trim()
    return previousLine.endsWith(":") && previousLine.dropLast(1).all { character -> character.isLetterOrDigit() || character == '_' }
}

private fun String.isAggregateOnlySelect(
    select: SourceSqlToken,
    statementTokens: List<SourceSqlToken>,
    statementEnd: Int,
): Boolean {
    val from = statementTokens.firstOrNull { token -> token.matches(SqlDialectSourceTerm.From) }
    val selectListEnd = from?.startOffset ?: statementEnd
    val selectListTokens = statementTokens.filter { token -> token.startOffset > select.endOffset && token.startOffset < selectListEnd }
    val first = selectListTokens.firstOrNull() ?: return false
    if (first.matches(SqlDialectSourceTerm.Exists)) return true
    if (from == null) return false
    if (!aggregateNames.contains(first.normalizedText)) return false
    return substring(first.endOffset, selectListEnd).contains(')') &&
        substring(from.endOffset, statementEnd).isNotBlank()
}

private val aggregateNames = setOf("count", "min", "max")
