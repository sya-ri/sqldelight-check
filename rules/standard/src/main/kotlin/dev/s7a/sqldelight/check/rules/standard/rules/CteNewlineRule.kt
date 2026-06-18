package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatterns
import dev.s7a.sqldelight.check.api.SqlDialectSourceTerm
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports multiline WITH clauses whose CTE definitions do not start their own line.
 */
public class CteNewlineRule : Rule {
    override val id: RuleId = RuleId("cte-newline")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        val lines = content.linesWithRanges()
        val tokens = content.sqlTokens().toList()
        tokens.forEachIndexed { index, token ->
            if (!token.isTerm(SqlDialectSourceTerm.With)) return@forEachIndexed
            val depth = content.sqlParenthesisDepthAt(token.startOffset)
            if (depth != 0) return@forEachIndexed
            val statementEnd = content.statementEndAfter(token.startOffset)
            val mainStatementIndex =
                tokens.mainStatementIndexAfterWith(
                    startIndex = index + 1,
                    statementEnd = statementEnd,
                    depth = depth,
                    content = content,
                    sourcePatterns = context.database.dialect.sourcePatterns,
                )
                ?: return@forEachIndexed
            val cteStarts = tokens.cteStartTokens(index + 1, mainStatementIndex, depth, content)
            if (cteStarts.size < 2) return@forEachIndexed
            if (!content.substring(token.endOffset, tokens[mainStatementIndex].startOffset).contains('\n')) return@forEachIndexed
            if (cteStarts.all { cte ->
                    lines.lineContaining(cte.startOffset)?.firstNonWhitespaceOffset == cte.startOffset
                }
            ) {
                return@forEachIndexed
            }
            val misplacedCteStarts =
                cteStarts
                    .filter { cte -> lines.lineContaining(cte.startOffset)?.firstNonWhitespaceOffset != cte.startOffset }
                    .map { cte -> cte.startOffset }

            reporter.report(
                RuleDiagnostic(
                    severity = defaultSeverity,
                    message = "Multiline WITH clauses should put each CTE definition on its own line.",
                    file = context.file,
                    range = content.rangeAtOffsets(token.startOffset, tokens[mainStatementIndex].startOffset),
                    database = context.database,
                    fixes = listOf(content.startOwnLineFix(misplacedCteStarts, "Move CTE definitions to their own lines")),
                ),
            )
        }
    }
}

private fun List<SqlToken>.mainStatementIndexAfterWith(
    startIndex: Int,
    statementEnd: Int,
    depth: Int,
    content: String,
    sourcePatterns: SqlDialectSourcePatterns,
): Int? {
    var sawCteBody = false
    for (index in startIndex until size) {
        val token = this[index]
        if (token.startOffset >= statementEnd) return null
        if (content.sqlParenthesisDepthAt(token.startOffset) != depth) continue
        if (token.isTerm(SqlDialectSourceTerm.As)) continue
        if (content.previousSqlCharacterBefore(token.startOffset)?.value == ')') {
            sawCteBody = true
        }
        if (sawCteBody && token.matches(sourcePatterns, SqlDialectSourcePatternRole.SqlDelightExecutableStatementStart)) return index
    }
    return null
}

private fun List<SqlToken>.cteStartTokens(
    startIndex: Int,
    endIndex: Int,
    depth: Int,
    content: String,
): List<SqlToken> {
    val starts = mutableListOf<SqlToken>()
    getOrNull(startIndex)?.let(starts::add)
    for (index in startIndex until endIndex) {
        val token = this[index]
        if (content.sqlParenthesisDepthAt(token.startOffset) != depth) continue
        if (content.previousSqlCharacterBefore(token.startOffset)?.value == ',') {
            starts += token
        }
    }
    return starts
}
