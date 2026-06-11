package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports bare conditioned joins that should be written as `INNER JOIN`.
 */
public class ExplicitInnerJoinRule : Rule {
    override val id: RuleId = RuleId("explicit-inner-join")
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
            if (tokens.hasJoinTypePrefix(index)) return@forEachIndexed
            if (!tokens.hasJoinCondition(index, content)) return@forEachIndexed

            reporter.report(
                Diagnostic(
                    ruleId = id,
                    severity = defaultSeverity,
                    message = "Use INNER JOIN instead of bare JOIN when ON or USING is present.",
                    file = context.file,
                    range = content.rangeAtOffsets(token.startOffset, token.endOffset),
                    database = context.database,
                ),
            )
        }
    }
}

private fun List<SqlToken>.hasJoinTypePrefix(joinIndex: Int): Boolean {
    val previous = getOrNull(joinIndex - 1)?.normalizedText ?: return false
    if (previous in explicitJoinPrefixes) return true
    if (previous != "outer") return false
    return getOrNull(joinIndex - 2)?.normalizedText in outerJoinPrefixes
}

private fun List<SqlToken>.hasJoinCondition(
    joinIndex: Int,
    content: String,
): Boolean {
    val join = get(joinIndex)
    val joinDepth = content.sqlParenthesisDepthAt(join.startOffset)
    val statementEnd = content.statementEndAfter(join.startOffset)
    val segmentEnd = conditionedJoinSegmentEnd(joinIndex + 1, statementEnd, joinDepth, content)
    return asSequence()
        .drop(joinIndex + 1)
        .takeWhile { token -> token.startOffset < segmentEnd }
        .filter { token -> content.sqlParenthesisDepthAt(token.startOffset) == joinDepth }
        .any { token -> token.isKeyword("on") || token.isKeyword("using") }
}

private fun List<SqlToken>.conditionedJoinSegmentEnd(
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
                (token.isKeyword("join") || token.normalizedText in conditionedJoinBoundaryKeywords)
        }
        ?.startOffset
        ?: statementEnd

private val explicitJoinPrefixes = setOf("cross", "full", "inner", "left", "natural", "right")

private val outerJoinPrefixes = setOf("full", "left", "right")

private val conditionedJoinBoundaryKeywords =
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
