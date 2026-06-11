package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports comparison and binary operators that lead a line in multiline SQL.
 */
public class OperatorLinePositionRule : Rule {
    override val id: RuleId = RuleId("standard:operator-line-position")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnablement: Enablement = Enablement.Auto

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        if (!content.contains('\n')) return

        val lines = content.linesWithRanges()
        val tokens = content.sqlTokens().toList()
        content
            .sqlCharacters()
            .mapNotNull { character -> content.linePositionOperatorAt(character.offset) }
            .distinctBy { operator -> operator.startOffset }
            .forEach { operator ->
                val line = lines.lineContaining(operator.startOffset) ?: return@forEach
                if (line.firstNonWhitespaceOffset != operator.startOffset) return@forEach
                if (content.shouldSkipLinePositionOperator(operator, tokens)) return@forEach

                val operatorText = content.substring(operator.startOffset, operator.endOffset)
                reporter.report(
                    Diagnostic(
                        ruleId = id,
                        severity = defaultSeverity,
                        message = "Operator '$operatorText' should trail the previous line.",
                        file = context.file,
                        range = content.rangeAtOffsets(operator.startOffset, operator.endOffset),
                        database = context.database,
                    ),
                )
            }
    }
}

private fun String.shouldSkipLinePositionOperator(
    operator: LinePositionOperator,
    tokens: List<SqlToken>,
): Boolean {
    val operatorText = substring(operator.startOffset, operator.endOffset)
    val previousToken = tokens.lastOrNull { token -> token.endOffset <= operator.startOffset }
    val previous = previousNonWhitespaceBefore(operator.startOffset)
    if (operatorText in setOf("+", "-") && previousToken?.normalizedText in unaryLineOperatorPrecedingKeywords) {
        return true
    }
    if (operatorText in setOf("+", "-") && previous in setOf('(', ',')) return true
    if (operatorText == "*" && previousToken?.isKeyword("select") == true) return true

    return operatorText == "*" && previous in setOf('(', ',', '.')
}

private data class LinePositionOperator(
    val startOffset: Int,
    val endOffset: Int,
)

private fun String.linePositionOperatorAt(offset: Int): LinePositionOperator? {
    val comparison = comparisonOperatorAt(offset)
    if (comparison != null) {
        return LinePositionOperator(startOffset = comparison.startOffset, endOffset = comparison.endOffset)
    }

    val binary = binaryOperatorAt(offset)
    return if (binary == null) {
        null
    } else {
        LinePositionOperator(startOffset = binary.startOffset, endOffset = binary.endOffset)
    }
}

private fun String.previousNonWhitespaceBefore(offset: Int): Char? {
    var index = offset - 1
    while (index >= 0 && this[index].isWhitespace()) {
        index--
    }
    return getOrNull(index)
}

private val unaryLineOperatorPrecedingKeywords =
    setOf(
        "and",
        "by",
        "else",
        "or",
        "select",
        "set",
        "then",
        "values",
        "when",
        "where",
    )
