package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

import dev.s7a.sqldelight.check.api.Fix
import dev.s7a.sqldelight.check.api.FixSafety
import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatterns
import dev.s7a.sqldelight.check.api.SqlDialectSourceTerm
import dev.s7a.sqldelight.check.api.TextEdit
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
        val parenthesisDepths = content.computeParenthesisDepths()
        tokens.forEachIndexed { index, token ->
            if (!token.isTerm(SqlDialectSourceTerm.Join)) return@forEachIndexed
            if (tokens.hasJoinTypePrefix(index)) return@forEachIndexed
            if (!tokens.hasJoinCondition(index, content, parenthesisDepths, context.database.dialect.sourcePatterns)) return@forEachIndexed

            reporter.report(
                RuleDiagnostic(
                    severity = defaultSeverity,
                    message = "Use INNER JOIN instead of bare JOIN when ON or USING is present.",
                    file = context.file,
                    range = content.rangeAtOffsets(token.startOffset, token.endOffset),
                    database = context.database,
                    fixes =
                        listOf(
                            Fix(
                                title = "Use INNER JOIN",
                                safety = FixSafety.Unsafe,
                                edits =
                                    listOf(
                                        TextEdit(
                                            range = content.rangeAtOffsets(token.startOffset, token.startOffset),
                                            replacement = "INNER ",
                                        ),
                                    ),
                            ),
                        ),
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
    parenthesisDepths: IntArray,
    sourcePatterns: SqlDialectSourcePatterns,
): Boolean {
    val join = get(joinIndex)
    val joinDepth = parenthesisDepths[join.startOffset]
    val statementEnd = content.statementEndAfter(join.startOffset)
    val segmentEnd = conditionedJoinSegmentEnd(joinIndex + 1, statementEnd, joinDepth, parenthesisDepths, sourcePatterns)
    return asSequence()
        .drop(joinIndex + 1)
        .takeWhile { token -> token.startOffset < segmentEnd }
        .filter { token -> parenthesisDepths[token.startOffset] == joinDepth }
        .any { token -> token.isTerm(SqlDialectSourceTerm.On) || token.isTerm(SqlDialectSourceTerm.Using) }
}

private fun List<SqlToken>.conditionedJoinSegmentEnd(
    startIndex: Int,
    statementEnd: Int,
    joinDepth: Int,
    parenthesisDepths: IntArray,
    sourcePatterns: SqlDialectSourcePatterns,
): Int =
    asSequence()
        .drop(startIndex)
        .withIndex()
        .firstOrNull { (relativeIndex, token) ->
            token.startOffset < statementEnd &&
                parenthesisDepths[token.startOffset] == joinDepth &&
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
