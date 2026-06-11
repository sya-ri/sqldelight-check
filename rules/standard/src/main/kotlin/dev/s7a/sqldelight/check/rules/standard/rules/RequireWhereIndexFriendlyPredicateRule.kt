package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports common function-wrapped WHERE predicates that are hard to use with indexes.
 */
public class RequireWhereIndexFriendlyPredicateRule : Rule {
    override val id: RuleId = RuleId("require-where-index-friendly-predicate")
    override val defaultSeverity: Severity = Severity.Info
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        val tokens = content.sqlTokens().toList()
        tokens.forEachIndexed { index, token ->
            if (!token.isKeyword("where")) return@forEachIndexed
            val statementEnd = content.statementEndAfter(token.startOffset)
            val boundary = tokens.firstBoundaryOffsetAfter(index + 1, statementEnd, wherePredicateBoundaryKeywords)
            tokens
                .drop(index + 1)
                .takeWhile { candidate -> candidate.startOffset < boundary }
                .filter { candidate -> candidate.normalizedText in nonSargableFunctionNames }
                .filter { candidate -> content.functionCallFeedsComparison(candidate) }
                .forEach { function ->
                    reporter.report(
                        RuleDiagnostic(
                            severity = defaultSeverity,
                            message = "Avoid wrapping WHERE columns in functions when an index-friendly predicate can be used.",
                            file = context.file,
                            range = content.rangeAtOffsets(function.startOffset, function.endOffset),
                            database = context.database,
                        ),
                    )
                }
        }
    }
}

private val nonSargableFunctionNames = setOf("coalesce", "date", "ifnull", "lower", "substr", "substring", "trim", "upper")

private val wherePredicateBoundaryKeywords = setOf("except", "group", "having", "intersect", "limit", "offset", "order", "union", "window")

private fun String.functionCallFeedsComparison(function: SqlToken): Boolean {
    val open = nextNonHorizontalWhitespaceOffset(function.endOffset) ?: return false
    if (getOrNull(open) != '(') return false
    val close = matchingClosingParenthesisOffset(open) ?: return false
    val next = nextSqlCharacterAfter(close + 1) ?: return false
    return next.value in setOf('=', '<', '>', '!')
}
