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
 * Reports predicates that mix same-level AND and OR without explicit grouping.
 */
public class RequireParenthesesForMixedBooleanOperatorsRule : Rule {
    override val id: RuleId = RuleId("require-parentheses-for-mixed-boolean-operators")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        val parenthesisDepths = content.computeParenthesisDepths()
        val tokens = content.sqlTokens().toList()
        tokens.forEachIndexed { index, token ->
            if (!token.matches(context.database.dialect.sourcePatterns, SqlDialectSourcePatternRole.PredicateStart)) {
                return@forEachIndexed
            }

            val clauseEnd = tokens.predicateBoundaryOffsetAfter(
                content = content,
                startIndex = index + 1,
                statementEnd = content.statementEndAfter(token.startOffset),
                depth = parenthesisDepths[token.startOffset],
                sourcePatterns = context.database.dialect.sourcePatterns,
                role = token.predicateBoundaryRole(),
                parenthesisDepths = parenthesisDepths,
            )
            tokens
                .subList(index + 1, tokens.indexAfterOffset(index + 1, clauseEnd))
                .mixedBooleanOperators(content, clauseEnd, context.database.dialect.sourcePatterns, parenthesisDepths)
                .forEach { mixed ->
                    reporter.report(
                        RuleDiagnostic(
                            severity = defaultSeverity,
                            message = "Use parentheses when mixing AND and OR in the same predicate.",
                            file = context.file,
                            range = content.rangeAtOffsets(mixed.startOffset, mixed.endOffset),
                            database = context.database,
                        ),
                    )
                }
        }
    }
}

private data class MixedBooleanOperators(
    val startOffset: Int,
    val endOffset: Int,
)

private data class BooleanOperatorState(
    val operator: SqlDialectSourceTerm,
    val startOffset: Int,
)

private fun List<SqlToken>.mixedBooleanOperators(
    content: String,
    clauseEnd: Int,
    sourcePatterns: SqlDialectSourcePatterns,
    parenthesisDepths: IntArray,
): List<MixedBooleanOperators> {
    val statesByDepth = mutableMapOf<Int, BooleanOperatorState>()
    val reportedDepths = mutableSetOf<Int>()
    val pendingBetweenDepths = mutableSetOf<Int>()
    val mixedOperators = mutableListOf<MixedBooleanOperators>()

    filter { token -> token.startOffset < clauseEnd }
        .forEach { token ->
            val depth = parenthesisDepths[token.startOffset]
            when {
                token.isTerm(SqlDialectSourceTerm.Between) -> pendingBetweenDepths += depth
                token.isTerm(SqlDialectSourceTerm.And) && depth in pendingBetweenDepths -> pendingBetweenDepths -= depth
                token.matches(sourcePatterns, SqlDialectSourcePatternRole.BooleanOperator) && depth !in reportedDepths -> {
                    val operator = booleanOperatorTerms.firstOrNull { term -> token.isTerm(term) } ?: return@forEach
                    val previous = statesByDepth[depth]
                    if (previous == null) {
                        statesByDepth[depth] =
                            BooleanOperatorState(
                                operator = operator,
                                startOffset = token.startOffset,
                            )
                    } else if (previous.operator != operator) {
                        reportedDepths += depth
                        mixedOperators +=
                            MixedBooleanOperators(
                                startOffset = previous.startOffset,
                                endOffset = token.endOffset,
                            )
                    }
                }
            }
        }

    return mixedOperators
}

private fun List<SqlToken>.predicateBoundaryOffsetAfter(
    content: String,
    startIndex: Int,
    statementEnd: Int,
    depth: Int,
    sourcePatterns: SqlDialectSourcePatterns,
    role: SqlDialectSourcePatternRole,
    parenthesisDepths: IntArray,
): Int =
    asSequence()
        .drop(startIndex)
        .withIndex()
        .firstOrNull { (relativeIndex, token) ->
            token.startOffset < statementEnd &&
                parenthesisDepths[token.startOffset] == depth &&
                sourcePatterns.matches(role, normalizedTextsFrom(startIndex + relativeIndex))
        }
        ?.value
        ?.startOffset
        ?: statementEnd

private fun List<SqlToken>.indexAfterOffset(
    startIndex: Int,
    offset: Int,
): Int {
    for (index in startIndex until size) {
        if (this[index].startOffset >= offset) return index
    }
    return size
}

private fun SqlToken.predicateBoundaryRole(): SqlDialectSourcePatternRole =
    when {
        isTerm(SqlDialectSourceTerm.On) -> SqlDialectSourcePatternRole.JoinConditionBoundary
        else -> SqlDialectSourcePatternRole.PredicateBoundary
    }

private val booleanOperatorTerms = setOf(SqlDialectSourceTerm.And, SqlDialectSourceTerm.Or)
