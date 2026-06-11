package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SourceFileKind
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports repeated predicates on the same column that use different named parameters.
 */
public class ConsistentParameterNamesRule : Rule {
    override val id: RuleId = RuleId("consistent-parameter-names")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        if (context.file.kind != SourceFileKind.Query) return

        val content = context.file.content
        val seen = mutableMapOf<Triple<Int, String, String>, NamedParameterPredicate>()
        content.namedParameterPredicates().forEach { predicate ->
            val key = Triple(predicate.statementStartOffset, predicate.column.lowercase(), predicate.operator.lowercase())
            val previous = seen[key]
            if (previous == null) {
                seen[key] = predicate
            } else if (previous.parameter != predicate.parameter) {
                reporter.report(
                    RuleDiagnostic(
                        severity = defaultSeverity,
                        message = "Use the same named parameter for repeated ${predicate.column} predicates.",
                        file = context.file,
                        range = content.rangeAtOffsets(predicate.parameterStartOffset, predicate.parameterEndOffset),
                        database = context.database,
                    ),
                )
            }
        }
    }
}

private data class NamedParameterPredicate(
    val column: String,
    val operator: String,
    val parameter: String,
    val statementStartOffset: Int,
    val parameterStartOffset: Int,
    val parameterEndOffset: Int,
)

private fun String.namedParameterPredicates(): Sequence<NamedParameterPredicate> =
    sequence {
        val tokens = sqlTokens().toList()
        tokens.forEachIndexed { index, token ->
            val operator = nextComparisonOperatorAfter(token.endOffset) ?: return@forEachIndexed
            val parameter = namedParameterAfter(operator.endOffset) ?: return@forEachIndexed
            val previous = tokens.getOrNull(index - 1)?.normalizedText
            if (previous in setOf("from", "join", "as")) return@forEachIndexed
            yield(
                NamedParameterPredicate(
                    column = token.text,
                    operator = operator.text,
                    parameter = parameter.name,
                    statementStartOffset = statementStartBefore(token.startOffset),
                    parameterStartOffset = parameter.startOffset,
                    parameterEndOffset = parameter.endOffset,
                ),
            )
        }
    }

private data class ParameterComparisonOperator(
    val text: String,
    val endOffset: Int,
)

private data class NamedParameter(
    val name: String,
    val startOffset: Int,
    val endOffset: Int,
)

private fun String.nextComparisonOperatorAfter(offset: Int): ParameterComparisonOperator? {
    val index = nextNonHorizontalWhitespaceOffset(offset) ?: return null
    return when {
        startsWith(">=", index) -> ParameterComparisonOperator(">=", index + 2)
        startsWith("<=", index) -> ParameterComparisonOperator("<=", index + 2)
        startsWith("<>", index) -> ParameterComparisonOperator("<>", index + 2)
        startsWith("!=", index) -> ParameterComparisonOperator("!=", index + 2)
        getOrNull(index) == '=' -> ParameterComparisonOperator("=", index + 1)
        else -> null
    }
}

private fun String.statementStartBefore(offset: Int): Int {
    val previousSemicolon =
        sqlCharacters()
            .takeWhile { character -> character.offset < offset }
            .lastOrNull { character -> character.value == ';' }
            ?.offset
    return previousSemicolon?.plus(1) ?: 0
}

private fun String.namedParameterAfter(offset: Int): NamedParameter? {
    val start = nextNonHorizontalWhitespaceOffset(offset) ?: return null
    if (getOrNull(start) != ':') return null
    val nameStart = start + 1
    if (getOrNull(nameStart)?.isLetter() != true && getOrNull(nameStart) != '_') return null
    var end = nameStart + 1
    while (getOrNull(end)?.let { character -> character == '_' || character.isLetterOrDigit() } == true) {
        end++
    }
    return NamedParameter(name = substring(nameStart, end), startOffset = start, endOffset = end)
}
