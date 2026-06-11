package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
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
        val tokens = content.sqlTokens().toList()
        tokens.forEachIndexed { index, token ->
            if (!token.isKeyword("select")) return@forEachIndexed
            val selectDepth = content.sqlParenthesisDepthAt(token.startOffset)
            if (selectDepth != 0) return@forEachIndexed

            val statementEnd = content.statementEndAfter(token.startOffset)
            val statementTokens =
                tokens
                    .drop(index + 1)
                    .takeWhile { candidate -> candidate.startOffset < statementEnd }
                    .filter { candidate -> content.sqlParenthesisDepthAt(candidate.startOffset) == selectDepth }
            val limitOrOffset =
                statementTokens.firstOrNull { candidate -> candidate.isKeyword("limit") || candidate.isKeyword("offset") }
                    ?: return@forEachIndexed
            if (statementTokens.containsKeywordPair("order", "by")) return@forEachIndexed

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
