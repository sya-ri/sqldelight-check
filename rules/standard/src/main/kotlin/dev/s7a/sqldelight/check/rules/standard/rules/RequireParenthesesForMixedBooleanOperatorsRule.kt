package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
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
        val tokens = content.sqlTokens().toList()
        tokens.forEachIndexed { index, token ->
            if (token.normalizedText !in predicateStartKeywords) return@forEachIndexed

            val clauseEnd = tokens.predicateBoundaryOffsetAfter(
                content = content,
                startIndex = index + 1,
                statementEnd = content.statementEndAfter(token.startOffset),
                depth = content.sqlParenthesisDepthAt(token.startOffset),
                boundaryKeywords = token.predicateBoundaryKeywords(),
            )
            tokens
                .subList(index + 1, tokens.indexAfterOffset(index + 1, clauseEnd))
                .mixedBooleanOperators(content, clauseEnd)
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
    val operator: String,
    val startOffset: Int,
)

private fun List<SqlToken>.mixedBooleanOperators(
    content: String,
    clauseEnd: Int,
): List<MixedBooleanOperators> {
    val statesByDepth = mutableMapOf<Int, BooleanOperatorState>()
    val reportedDepths = mutableSetOf<Int>()
    val pendingBetweenDepths = mutableSetOf<Int>()
    val mixedOperators = mutableListOf<MixedBooleanOperators>()

    filter { token -> token.startOffset < clauseEnd }
        .forEach { token ->
            val depth = content.sqlParenthesisDepthAt(token.startOffset)
            when {
                token.isKeyword("between") -> pendingBetweenDepths += depth
                token.isKeyword("and") && depth in pendingBetweenDepths -> pendingBetweenDepths -= depth
                token.normalizedText in booleanOperators && depth !in reportedDepths -> {
                    val previous = statesByDepth[depth]
                    if (previous == null) {
                        statesByDepth[depth] =
                            BooleanOperatorState(
                                operator = token.normalizedText,
                                startOffset = token.startOffset,
                            )
                    } else if (previous.operator != token.normalizedText) {
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
    boundaryKeywords: Set<String>,
): Int =
    asSequence()
        .drop(startIndex)
        .firstOrNull { token ->
            token.startOffset < statementEnd &&
                content.sqlParenthesisDepthAt(token.startOffset) == depth &&
                token.normalizedText in boundaryKeywords
        }
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

private fun SqlToken.predicateBoundaryKeywords(): Set<String> =
    when (normalizedText) {
        "on" -> joinPredicateBoundaryKeywords
        else -> wherePredicateBoundaryKeywords
    }

private val predicateStartKeywords = setOf("where", "having", "on")

private val booleanOperators = setOf("and", "or")

private val wherePredicateBoundaryKeywords =
    setOf(
        "fetch",
        "group",
        "limit",
        "offset",
        "order",
        "union",
    )

private val joinPredicateBoundaryKeywords =
    wherePredicateBoundaryKeywords +
        setOf(
            "cross",
            "full",
            "inner",
            "join",
            "left",
            "right",
            "where",
        )
