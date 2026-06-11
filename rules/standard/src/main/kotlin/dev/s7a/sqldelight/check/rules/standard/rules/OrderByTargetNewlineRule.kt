package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports multiline ORDER BY clauses that do not put every ordering expression on its own line.
 */
public class OrderByTargetNewlineRule : Rule {
    override val id: RuleId = RuleId("order-by-target-newline")
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
            if (!token.isKeyword("order")) return@forEachIndexed
            val by = tokens.getOrNull(index + 1)?.takeIf { it.isKeyword("by") } ?: return@forEachIndexed
            val depth = content.sqlParenthesisDepthAt(token.startOffset)
            val statementEnd = content.statementEndAfter(token.startOffset)
            val clauseEnd =
                tokens.firstBoundaryOffsetAfterAtDepth(
                    content = content,
                    startIndex = index + 2,
                    statementEnd = statementEnd,
                    depth = depth,
                    boundaryKeywords = orderByTargetBoundaryKeywords,
                )
            val items = content.commaSeparatedClauseItems(by.endOffset, clauseEnd, depth)
            if (!content.isMultilineItemList(items)) return@forEachIndexed
            if (lines.itemStartsAreOnOwnLines(items)) return@forEachIndexed

            reporter.report(
                Diagnostic(
                    ruleId = id,
                    severity = defaultSeverity,
                    message = "Multiline ORDER BY clauses should put each ordering expression on its own line.",
                    file = context.file,
                    range = content.rangeAtOffsets(token.startOffset, clauseEnd),
                    database = context.database,
                ),
            )
        }
    }
}

private val orderByTargetBoundaryKeywords =
    setOf(
        "fetch",
        "limit",
        "offset",
        "union",
    )
