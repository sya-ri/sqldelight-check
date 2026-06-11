package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports set operations whose adjacent SELECT lists return different column counts.
 *
 * The rule compares statically countable SELECT lists on each side of `UNION`,
 * `INTERSECT`, and `EXCEPT`. Wildcard SELECT targets are left to
 * `standard:no-select-star` because they do not expose a reliable column count.
 */
public class ConsistentSetOperationColumnCountRule : Rule {
    override val id: RuleId = RuleId("consistent-set-operation-column-count")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        val tokens = content.sqlTokens().toList()
        tokens
            .filter { token -> token.normalizedText in setOperationKeywords }
            .forEach { operator ->
                val left = content.selectColumnCountBefore(operator, tokens) ?: return@forEach
                val right = content.selectColumnCountAfter(operator, tokens) ?: return@forEach
                if (left.count == null || right.count == null) return@forEach
                if (left.count == right.count) return@forEach

                reporter.report(
                    RuleDiagnostic(
                        severity = defaultSeverity,
                        message = "Set operation SELECT lists should return the same number of columns.",
                        file = context.file,
                        range = content.rangeAtOffsets(operator.startOffset, operator.endOffset),
                        database = context.database,
                    ),
                )
            }
    }
}

private data class SelectColumnCount(
    val count: Int?,
)

private data class SelectTargetRange(
    val startOffset: Int,
    val endOffset: Int,
)

private fun String.selectColumnCountBefore(
    operator: SqlToken,
    tokens: List<SqlToken>,
): SelectColumnCount? {
    val depth = sqlParenthesisDepthAt(operator.startOffset)
    val start = previousStatementBoundaryBefore(operator.startOffset)
    val select =
        tokens
            .asReversed()
            .firstOrNull { token ->
                token.startOffset >= start &&
                    token.startOffset < operator.startOffset &&
                    token.isKeyword("select") &&
                    sqlParenthesisDepthAt(token.startOffset) == depth
            } ?: return null
    return selectColumnCount(select, operator.startOffset, depth, tokens)
}

private fun String.selectColumnCountAfter(
    operator: SqlToken,
    tokens: List<SqlToken>,
): SelectColumnCount? {
    val depth = sqlParenthesisDepthAt(operator.startOffset)
    val end = statementEndAfter(operator.startOffset)
    val select =
        tokens
            .firstOrNull { token ->
                token.startOffset > operator.endOffset &&
                    token.startOffset < end &&
                    token.isKeyword("select") &&
                    sqlParenthesisDepthAt(token.startOffset) == depth
            } ?: return null
    val nextOperator =
        tokens
            .firstOrNull { token ->
                token.startOffset > select.endOffset &&
                    token.startOffset < end &&
                    token.normalizedText in setOperationKeywords &&
                    sqlParenthesisDepthAt(token.startOffset) == depth
            }
    return selectColumnCount(select, nextOperator?.startOffset ?: end, depth, tokens)
}

private fun String.selectColumnCount(
    select: SqlToken,
    segmentEndOffset: Int,
    depth: Int,
    tokens: List<SqlToken>,
): SelectColumnCount {
    val from =
        tokens
            .firstOrNull { token ->
                token.startOffset > select.endOffset &&
                    token.startOffset < segmentEndOffset &&
                    token.isKeyword("from") &&
                    sqlParenthesisDepthAt(token.startOffset) == depth
            }
    val listEnd = from?.startOffset ?: segmentEndOffset
    val targets = selectTargets(select.endOffset, listEnd, depth)
    return if (targets.any { target -> target.isWildcardIn(this) }) {
        SelectColumnCount(count = null)
    } else {
        SelectColumnCount(count = targets.size)
    }
}

private fun String.selectTargets(
    startOffset: Int,
    endOffset: Int,
    depth: Int,
): List<SelectTargetRange> {
    val targets = mutableListOf<SelectTargetRange>()
    var start = startOffset
    sqlCharacters()
        .dropWhile { character -> character.offset < startOffset }
        .takeWhile { character -> character.offset < endOffset }
        .forEach { character ->
            if (character.value == ',' && sqlParenthesisDepthAt(character.offset) == depth) {
                trimmedTarget(start, character.offset)?.let(targets::add)
                start = character.offset + 1
            }
        }
    trimmedTarget(start, endOffset)?.let(targets::add)
    return targets
}

private fun String.trimmedTarget(
    startOffset: Int,
    endOffset: Int,
): SelectTargetRange? {
    var start = startOffset
    var end = endOffset
    while (start < end && this[start].isWhitespace()) start++
    while (end > start && this[end - 1].isWhitespace()) end--
    return if (start < end) SelectTargetRange(startOffset = start, endOffset = end) else null
}

private fun SelectTargetRange.isWildcardIn(content: String): Boolean {
    val text = content.substring(startOffset, endOffset).trim()
    return text == "*" || text.endsWith(".*")
}

private fun String.previousStatementBoundaryBefore(offset: Int): Int =
    sqlCharacters()
        .takeWhile { character -> character.offset < offset }
        .lastOrNull { character -> character.value == ';' }
        ?.let { character -> character.offset + 1 }
        ?: 0

private val setOperationKeywords = setOf("except", "intersect", "union")
