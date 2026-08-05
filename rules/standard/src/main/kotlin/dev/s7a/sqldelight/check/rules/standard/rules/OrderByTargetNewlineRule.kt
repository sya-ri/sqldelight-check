package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole
import dev.s7a.sqldelight.check.api.SqlDialectSourceTerm
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
        val parenthesisDepths = content.computeParenthesisDepths()
        tokens.forEachIndexed { index, token ->
            if (!token.isTerm(SqlDialectSourceTerm.Order)) return@forEachIndexed
            val by = tokens.getOrNull(index + 1)?.takeIf { it.isTerm(SqlDialectSourceTerm.By) } ?: return@forEachIndexed
            val depth = parenthesisDepths[token.startOffset]
            val statementEnd = content.statementEndAfter(token.startOffset)
            val clauseEnd =
                tokens.firstBoundaryOffsetAfterAtDepth(
                    content = content,
                    startIndex = index + 2,
                    statementEnd = statementEnd,
                    depth = depth,
                    sourcePatterns = context.database.dialect.sourcePatterns,
                    role = SqlDialectSourcePatternRole.OrderByBoundary,
                    parenthesisDepths = parenthesisDepths,
                )
            val items = content.commaSeparatedClauseItems(by.endOffset, clauseEnd, depth, parenthesisDepths)
            if (!content.isMultilineItemList(items)) return@forEachIndexed
            if (lines.itemStartsAreOnOwnLines(items)) return@forEachIndexed
            val misplacedItemStarts = lines.misplacedItemStarts(items)

            reporter.report(
                RuleDiagnostic(
                    severity = defaultSeverity,
                    message = "Multiline ORDER BY clauses should put each ordering expression on its own line.",
                    file = context.file,
                    range = content.rangeAtOffsets(token.startOffset, clauseEnd),
                    database = context.database,
                    fixes = listOf(content.startOwnLineFix(misplacedItemStarts, "Move ORDER BY targets to their own lines")),
                ),
            )
        }
    }
}
