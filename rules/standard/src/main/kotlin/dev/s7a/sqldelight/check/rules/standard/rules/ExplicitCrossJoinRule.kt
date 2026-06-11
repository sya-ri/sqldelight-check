package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports join clauses that omit `ON` or `USING` instead of declaring `CROSS JOIN`.
 */
public class ExplicitCrossJoinRule : Rule {
    override val id: RuleId = RuleId("explicit-cross-join")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        val tokens = content.sqlTokens().toList()
        tokens.forEachIndexed { index, token ->
            if (!token.isKeyword("join")) return@forEachIndexed
            if (tokens.isExplicitConditionlessJoin(index)) return@forEachIndexed

            val joinDepth = content.sqlParenthesisDepthAt(token.startOffset)
            val statementEnd = content.statementEndAfter(token.startOffset)
            val segmentEnd = tokens.joinSegmentEnd(index + 1, statementEnd, joinDepth, content)
            val segmentTokens =
                tokens
                    .drop(index + 1)
                    .takeWhile { candidate -> candidate.startOffset < segmentEnd }
                    .filter { candidate -> content.sqlParenthesisDepthAt(candidate.startOffset) == joinDepth }
            if (segmentTokens.any { candidate -> candidate.isKeyword("on") || candidate.isKeyword("using") }) {
                return@forEachIndexed
            }

            reporter.report(
                RuleDiagnostic(
                    severity = defaultSeverity,
                    message = "Use CROSS JOIN for joins without ON or USING.",
                    file = context.file,
                    range = content.rangeAtOffsets(token.startOffset, token.endOffset),
                    database = context.database,
                ),
            )
        }
    }
}

private fun List<SqlToken>.isExplicitConditionlessJoin(joinIndex: Int): Boolean {
    val previous = getOrNull(joinIndex - 1)?.normalizedText
    return previous == "cross" || previous == "natural"
}

private fun List<SqlToken>.joinSegmentEnd(
    startIndex: Int,
    statementEnd: Int,
    joinDepth: Int,
    content: String,
): Int =
    asSequence()
        .drop(startIndex)
        .firstOrNull { token ->
            token.startOffset < statementEnd &&
                content.sqlParenthesisDepthAt(token.startOffset) == joinDepth &&
                (token.isKeyword("join") || token.normalizedText in joinBoundaryKeywords)
        }
        ?.startOffset
        ?: statementEnd

private val joinBoundaryKeywords =
    setOf(
        "except",
        "group",
        "having",
        "intersect",
        "limit",
        "offset",
        "order",
        "union",
        "where",
        "window",
    )
