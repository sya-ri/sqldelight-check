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
 * Reports multiline GROUP BY clauses that do not put every grouping expression on its own line.
 */
public class GroupByTargetNewlineRule : Rule {
    override val id: RuleId = RuleId("group-by-target-newline")
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
            if (!token.isTerm(SqlDialectSourceTerm.Group)) return@forEachIndexed
            val by = tokens.getOrNull(index + 1)?.takeIf { it.isTerm(SqlDialectSourceTerm.By) } ?: return@forEachIndexed
            val depth = content.sqlParenthesisDepthAt(token.startOffset)
            val statementEnd = content.statementEndAfter(token.startOffset)
            val clauseEnd =
                tokens.firstBoundaryOffsetAfterAtDepth(
                    content = content,
                    startIndex = index + 2,
                    statementEnd = statementEnd,
                    depth = depth,
                    sourcePatterns = context.database.dialect.sourcePatterns,
                    role = SqlDialectSourcePatternRole.GroupByBoundary,
                )
            val items = content.commaSeparatedClauseItems(by.endOffset, clauseEnd, depth)
            if (!content.isMultilineItemList(items)) return@forEachIndexed
            if (lines.itemStartsAreOnOwnLines(items)) return@forEachIndexed
            val misplacedItemStarts = lines.misplacedItemStarts(items)

            reporter.report(
                RuleDiagnostic(
                    severity = defaultSeverity,
                    message = "Multiline GROUP BY clauses should put each grouping expression on its own line.",
                    file = context.file,
                    range = content.rangeAtOffsets(token.startOffset, clauseEnd),
                    database = context.database,
                    fixes = listOf(content.startOwnLineFix(misplacedItemStarts, "Move GROUP BY targets to their own lines")),
                ),
            )
        }
    }
}
