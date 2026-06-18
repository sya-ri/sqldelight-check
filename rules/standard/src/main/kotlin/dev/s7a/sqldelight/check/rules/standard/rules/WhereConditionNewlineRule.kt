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
 * Reports multiline WHERE predicates whose same-level boolean operators do not start their own line.
 */
public class WhereConditionNewlineRule : Rule {
    override val id: RuleId = RuleId("where-condition-newline")
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
            if (!token.isTerm(SqlDialectSourceTerm.Where)) return@forEachIndexed
            val depth = content.sqlParenthesisDepthAt(token.startOffset)
            val statementEnd = content.statementEndAfter(token.startOffset)
            val clauseEnd =
                tokens.firstBoundaryOffsetAfterAtDepth(
                    content = content,
                    startIndex = index + 1,
                    statementEnd = statementEnd,
                    depth = depth,
                    sourcePatterns = context.database.dialect.sourcePatterns,
                    role = SqlDialectSourcePatternRole.PredicateBoundary,
                )
            if (!content.substring(token.endOffset, clauseEnd).contains('\n')) return@forEachIndexed

            var pendingBetween = false
            tokens
                .drop(index + 1)
                .takeWhile { candidate -> candidate.startOffset < clauseEnd }
                .filter { candidate -> content.sqlParenthesisDepthAt(candidate.startOffset) == depth }
                .forEach { candidate ->
                    when {
                        candidate.isTerm(SqlDialectSourceTerm.Between) -> pendingBetween = true
                        candidate.isTerm(SqlDialectSourceTerm.And) && pendingBetween -> pendingBetween = false
                        candidate.matches(context.database.dialect.sourcePatterns, SqlDialectSourcePatternRole.BooleanOperator) -> {
                            val line = lines.lineContaining(candidate.startOffset) ?: return@forEach
                            if (line.firstNonWhitespaceOffset == candidate.startOffset) return@forEach
                            reporter.report(
                                RuleDiagnostic(
                                    severity = defaultSeverity,
                                    message = "Multiline WHERE boolean operators should start their own line.",
                                    file = context.file,
                                    range = content.rangeAtOffsets(candidate.startOffset, candidate.endOffset),
                                    database = context.database,
                                    fixes = listOf(content.startOwnLineFix(candidate.startOffset, "Move boolean operator to its own line")),
                                ),
                            )
                        }
                    }
                }
        }
    }
}
