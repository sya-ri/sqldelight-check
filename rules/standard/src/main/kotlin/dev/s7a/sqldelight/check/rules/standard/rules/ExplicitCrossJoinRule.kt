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
        val parenthesisDepths = content.computeParenthesisDepths()
        tokens.forEachIndexed { index, token ->
            if (!token.isTerm(SqlDialectSourceTerm.Join)) return@forEachIndexed
            if (tokens.isExplicitConditionlessJoin(index)) return@forEachIndexed

            val joinDepth = parenthesisDepths[token.startOffset]
            val statementEnd = content.statementEndAfter(token.startOffset)
            val segmentEnd =
                tokens.joinSegmentEnd(index + 1, statementEnd, joinDepth, parenthesisDepths, context.database.dialect.sourcePatterns)
            val segmentTokens =
                tokens
                    .drop(index + 1)
                    .takeWhile { candidate -> candidate.startOffset < segmentEnd }
                    .filter { candidate -> parenthesisDepths[candidate.startOffset] == joinDepth }
            if (segmentTokens.any { candidate -> candidate.isTerm(SqlDialectSourceTerm.On) || candidate.isTerm(SqlDialectSourceTerm.Using) }) {
                return@forEachIndexed
            }

            reporter.report(
                RuleDiagnostic(
                    severity = defaultSeverity,
                    message = "Use CROSS JOIN for joins without ON or USING.",
                    file = context.file,
                    range = content.rangeAtOffsets(token.startOffset, token.endOffset),
                    database = context.database,
                    fixes = listOf(content.crossJoinFix(tokens, index)),
                ),
            )
        }
    }
}

private fun String.crossJoinFix(
    tokens: List<SqlToken>,
    joinIndex: Int,
) = tokens.conditionlessJoinStartOffset(joinIndex)?.let { startOffset ->
    replaceTokenFix(startOffset, tokens[joinIndex].endOffset, "CROSS JOIN", "Use CROSS JOIN")
} ?: insertTokenFix(tokens[joinIndex].startOffset, "CROSS ", "Use CROSS JOIN")

private fun List<SqlToken>.conditionlessJoinStartOffset(joinIndex: Int): Int? {
    val previous = getOrNull(joinIndex - 1) ?: return null
    if (conditionlessJoinPrefixTerms.any { term -> previous.isTerm(term) }) return previous.startOffset
    if (!previous.isTerm(SqlDialectSourceTerm.Outer)) return null
    val beforeOuter = getOrNull(joinIndex - 2) ?: return null
    return if (outerConditionlessJoinPrefixTerms.any { term -> beforeOuter.isTerm(term) }) beforeOuter.startOffset else null
}

private fun List<SqlToken>.isExplicitConditionlessJoin(joinIndex: Int): Boolean {
    val previous = getOrNull(joinIndex - 1) ?: return false
    return previous.isTerm(SqlDialectSourceTerm.Cross) || previous.isTerm(SqlDialectSourceTerm.Natural)
}

private val conditionlessJoinPrefixTerms =
    setOf(
        SqlDialectSourceTerm.Full,
        SqlDialectSourceTerm.Inner,
        SqlDialectSourceTerm.Left,
        SqlDialectSourceTerm.Right,
    )

private val outerConditionlessJoinPrefixTerms = setOf(SqlDialectSourceTerm.Full, SqlDialectSourceTerm.Left, SqlDialectSourceTerm.Right)

private fun List<SqlToken>.joinSegmentEnd(
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
