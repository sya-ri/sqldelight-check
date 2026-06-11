package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
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
            if (!token.isKeyword("where")) return@forEachIndexed
            val depth = content.sqlParenthesisDepthAt(token.startOffset)
            val statementEnd = content.statementEndAfter(token.startOffset)
            val clauseEnd =
                tokens.firstBoundaryOffsetAfterAtDepth(
                    content = content,
                    startIndex = index + 1,
                    statementEnd = statementEnd,
                    depth = depth,
                    boundaryKeywords = whereConditionBoundaryKeywords,
                )
            if (!content.substring(token.endOffset, clauseEnd).contains('\n')) return@forEachIndexed

            var pendingBetween = false
            tokens
                .drop(index + 1)
                .takeWhile { candidate -> candidate.startOffset < clauseEnd }
                .filter { candidate -> content.sqlParenthesisDepthAt(candidate.startOffset) == depth }
                .forEach { candidate ->
                    when {
                        candidate.isKeyword("between") -> pendingBetween = true
                        candidate.isKeyword("and") && pendingBetween -> pendingBetween = false
                        candidate.normalizedText in booleanOperatorsForWhere -> {
                            val line = lines.lineContaining(candidate.startOffset) ?: return@forEach
                            if (line.firstNonWhitespaceOffset == candidate.startOffset) return@forEach
                            reporter.report(
                                RuleDiagnostic(
                                    severity = defaultSeverity,
                                    message = "Multiline WHERE boolean operators should start their own line.",
                                    file = context.file,
                                    range = content.rangeAtOffsets(candidate.startOffset, candidate.endOffset),
                                    database = context.database,
                                ),
                            )
                        }
                    }
                }
        }
    }
}

private val booleanOperatorsForWhere = setOf("and", "or")

private val whereConditionBoundaryKeywords =
    setOf(
        "group",
        "having",
        "limit",
        "offset",
        "order",
        "union",
    )
