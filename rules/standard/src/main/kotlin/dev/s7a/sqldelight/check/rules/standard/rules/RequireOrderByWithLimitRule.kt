package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SqlDialectSourceTerm
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports top-level `SELECT` statements that use `LIMIT` or `OFFSET` without `ORDER BY`.
 */
public class RequireOrderByWithLimitRule : Rule {
    override val id: RuleId = RuleId("require-order-by-with-limit")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        val parenthesisDepths = content.computeParenthesisDepths()
        val tokens = content.sqlTokens().toList()
        tokens.forEachIndexed { index, token ->
            if (!token.isTerm(SqlDialectSourceTerm.Select)) return@forEachIndexed
            if (parenthesisDepths[token.startOffset] != 0) return@forEachIndexed

            val statementEnd = content.statementEndAfter(token.startOffset)
            val statementTokens =
                tokens
                    .drop(index + 1)
                    .takeWhile { candidate -> candidate.startOffset < statementEnd }
                    .filter { candidate -> parenthesisDepths[candidate.startOffset] == 0 }
            val limitOrOffset =
                statementTokens.firstOrNull { candidate ->
                    candidate.isTerm(SqlDialectSourceTerm.Limit) || candidate.isTerm(SqlDialectSourceTerm.Offset)
                }
                    ?: return@forEachIndexed
            if (statementTokens.containsTermPair(SqlDialectSourceTerm.Order, SqlDialectSourceTerm.By)) return@forEachIndexed

            reporter.report(
                RuleDiagnostic(
                    severity = defaultSeverity,
                    message = "SELECT statements with LIMIT or OFFSET should also specify ORDER BY.",
                    file = context.file,
                    range = content.rangeAtOffsets(limitOrOffset.startOffset, limitOrOffset.endOffset),
                    database = context.database,
                ),
            )
        }
    }
}
