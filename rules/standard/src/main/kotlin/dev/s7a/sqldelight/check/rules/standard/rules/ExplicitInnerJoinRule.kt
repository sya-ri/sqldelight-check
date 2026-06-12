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
            if (!token.isTerm(SqlDialectSourceTerm.Join)) return@forEachIndexed
            if (tokens.hasJoinTypePrefix(index)) return@forEachIndexed
            if (!tokens.hasJoinCondition(index, content, context.database.dialect.sourcePatterns)) return@forEachIndexed

            reporter.report(
                RuleDiagnostic(
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
    val previous = getOrNull(joinIndex - 1) ?: return false
    if (explicitJoinPrefixTerms.any { term -> previous.isTerm(term) }) return true
    if (!previous.isTerm(SqlDialectSourceTerm.Outer)) return false
    val beforeOuter = getOrNull(joinIndex - 2) ?: return false
    return outerJoinPrefixTerms.any { term -> beforeOuter.isTerm(term) }
}

private fun List<SqlToken>.hasJoinCondition(
    joinIndex: Int,
    content: String,
    sourcePatterns: SqlDialectSourcePatterns,
): Boolean {
    val join = get(joinIndex)
    val joinDepth = content.sqlParenthesisDepthAt(join.startOffset)
    val statementEnd = content.statementEndAfter(join.startOffset)
    val segmentEnd = conditionedJoinSegmentEnd(joinIndex + 1, statementEnd, joinDepth, content, sourcePatterns)
    return asSequence()
        .drop(joinIndex + 1)
        .takeWhile { token -> token.startOffset < segmentEnd }
        .filter { token -> content.sqlParenthesisDepthAt(token.startOffset) == joinDepth }
        .any { token -> token.isTerm(SqlDialectSourceTerm.On) || token.isTerm(SqlDialectSourceTerm.Using) }
}

private fun List<SqlToken>.conditionedJoinSegmentEnd(
    startIndex: Int,
    statementEnd: Int,
    joinDepth: Int,
    content: String,
    sourcePatterns: SqlDialectSourcePatterns,
): Int =
    asSequence()
        .drop(startIndex)
        .withIndex()
        .firstOrNull { (relativeIndex, token) ->
            token.startOffset < statementEnd &&
                content.sqlParenthesisDepthAt(token.startOffset) == joinDepth &&
                (
                    token.isTerm(SqlDialectSourceTerm.Join) ||
                        sourcePatterns.matches(
                            SqlDialectSourcePatternRole.JoinConditionBoundary,
                            normalizedTextsFrom(startIndex + relativeIndex),
                        )
                )
        }
        ?.value
        ?.startOffset
        ?: statementEnd

private val explicitJoinPrefixTerms =
    setOf(
        SqlDialectSourceTerm.Cross,
        SqlDialectSourceTerm.Full,
        SqlDialectSourceTerm.Inner,
        SqlDialectSourceTerm.Left,
        SqlDialectSourceTerm.Natural,
        SqlDialectSourceTerm.Right,
    )

private val outerJoinPrefixTerms = setOf(SqlDialectSourceTerm.Full, SqlDialectSourceTerm.Left, SqlDialectSourceTerm.Right)
