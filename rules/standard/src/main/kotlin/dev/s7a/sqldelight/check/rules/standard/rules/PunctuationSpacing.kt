package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.Fix
import dev.s7a.sqldelight.check.api.FixSafety
import dev.s7a.sqldelight.check.api.SqlDialectSourceTerm
import dev.s7a.sqldelight.check.api.TextEdit
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

internal data class SqlOperator(
    val startOffset: Int,
    val endOffset: Int,
)

internal fun String.reportNoSpaceBeforeToken(
    context: RuleContext,
    reporter: DiagnosticReporter,
    rule: Rule,
    token: Char,
    message: String,
    fixTitle: String,
) {
    sqlCharacters()
        .filter { character -> character.value == token }
        .forEach { character ->
            val whitespaceStart = horizontalWhitespaceStartBefore(character.offset)
            if (whitespaceStart == character.offset) return@forEach
            if (whitespaceStart > 0 && this[whitespaceStart - 1] in setOf('\n', '\r')) return@forEach

            val range = rangeAtOffsets(whitespaceStart, character.offset)
            reporter.report(
                RuleDiagnostic(
                    severity = rule.defaultSeverity,
                    message = message,
                    file = context.file,
                    range = range,
                    database = context.database,
                    fixes =
                        listOf(
                            Fix(
                                title = fixTitle,
                                safety = FixSafety.Safe,
                                edits = listOf(TextEdit(range = range, replacement = "")),
                            ),
                        ),
                ),
            )
        }
}

internal fun String.horizontalWhitespaceStartBefore(offset: Int): Int {
    var index = offset
    while (index > 0 && this[index - 1].isHorizontalWhitespace()) {
        index--
    }
    return index
}

internal fun String.horizontalWhitespaceEndAfter(offset: Int): Int {
    var index = offset
    while (index < length && this[index].isHorizontalWhitespace()) {
        index++
    }
    return index
}

internal fun String.reportOperatorSpacing(
    context: RuleContext,
    reporter: DiagnosticReporter,
    rule: Rule,
    operators: Sequence<SqlOperator>,
    canNormalize: (leftStart: Int, operatorStart: Int, operatorEnd: Int, rightEnd: Int) -> Boolean,
    message: (operatorText: String) -> String,
    fixTitle: String,
) {
    operators
        .distinctBy { operator -> operator.startOffset }
        .forEach { operator ->
            val leftStart = horizontalWhitespaceStartBefore(operator.startOffset)
            val rightEnd = horizontalWhitespaceEndAfter(operator.endOffset)
            if (!canNormalize(leftStart, operator.startOffset, operator.endOffset, rightEnd)) return@forEach

            val operatorText = substring(operator.startOffset, operator.endOffset)
            val replacement = " $operatorText "
            if (substring(leftStart, rightEnd) == replacement) return@forEach

            val range = rangeAtOffsets(leftStart, rightEnd)
            reporter.report(
                RuleDiagnostic(
                    severity = rule.defaultSeverity,
                    message = message(operatorText),
                    file = context.file,
                    range = range,
                    database = context.database,
                    fixes =
                        listOf(
                            Fix(
                                title = fixTitle,
                                safety = FixSafety.Unsafe,
                                edits = listOf(TextEdit(range = range, replacement = replacement)),
                            ),
                        ),
                ),
            )
        }
}

internal fun String.comparisonOperatorAt(offset: Int): SqlOperator? {
    if (offset >= length) return null
    return when (this[offset]) {
        '=' ->
            when {
                offset > 0 && this[offset - 1].isComparisonOperatorCharacter() -> null
                offset + 1 < length && this[offset + 1].isComparisonOperatorCharacter() -> null
                else -> SqlOperator(startOffset = offset, endOffset = offset + 1)
            }
        '!' ->
            if (offset + 1 < length && this[offset + 1] == '=') {
                SqlOperator(startOffset = offset, endOffset = offset + 2)
            } else {
                null
            }
        '<' ->
            when {
                offset + 1 < length && this[offset + 1] in setOf('=', '>') ->
                    SqlOperator(startOffset = offset, endOffset = offset + 2)
                offset + 1 < length && this[offset + 1].isComparisonOperatorCharacter() -> null
                else -> SqlOperator(startOffset = offset, endOffset = offset + 1)
            }
        '>' ->
            when {
                offset > 0 && this[offset - 1].isComparisonOperatorCharacter() -> null
                offset + 1 < length && this[offset + 1] == '=' ->
                    SqlOperator(startOffset = offset, endOffset = offset + 2)
                offset + 1 < length && this[offset + 1].isComparisonOperatorCharacter() -> null
                else -> SqlOperator(startOffset = offset, endOffset = offset + 1)
            }
        else -> null
    }
}

internal fun String.canNormalizeInlineSpacing(
    leftStart: Int,
    operatorStart: Int,
    operatorEnd: Int,
    rightEnd: Int,
): Boolean {
    if (leftStart == 0 || rightEnd >= length) return false
    if (this[leftStart - 1] == '\r' || this[leftStart - 1] == '\n') return false
    if (this[operatorEnd] == '\r' || this[operatorEnd] == '\n') return false
    if (this[rightEnd] == '\r' || this[rightEnd] == '\n') return false
    if (operatorStart > 0 && this[operatorStart - 1] in setOf('<', '>', '!', '=')) return false
    return true
}

internal fun String.binaryOperatorAt(offset: Int): SqlOperator? {
    if (offset >= length) return null
    return when {
        startsWith("||", offset) -> SqlOperator(startOffset = offset, endOffset = offset + 2)
        this[offset] in setOf('*', '/', '%') -> SqlOperator(startOffset = offset, endOffset = offset + 1)
        this[offset] == '+' && !isUnarySign(offset) -> SqlOperator(startOffset = offset, endOffset = offset + 1)
        this[offset] == '-' && offset + 1 < length && this[offset + 1] == '>' -> null
        this[offset] == '-' && !isUnarySign(offset) -> SqlOperator(startOffset = offset, endOffset = offset + 1)
        else -> null
    }
}

internal fun String.canNormalizeBinaryOperatorSpacing(
    leftStart: Int,
    operatorStart: Int,
    operatorEnd: Int,
    rightEnd: Int,
): Boolean {
    if (!canNormalizeInlineSpacing(leftStart, operatorStart, operatorEnd, rightEnd)) return false
    val left = previousNonHorizontalWhitespace(operatorStart) ?: return false
    val right = nextNonHorizontalWhitespace(operatorEnd) ?: return false
    if (left in setOf('(', ',', '=', '<', '>', '!', '+', '-', '*', '/', '%', '|')) return false
    if (right in setOf(')', ',', '=', '<', '>', '!', '*', '/', '%', '|')) return false
    return true
}

private fun String.isUnarySign(offset: Int): Boolean {
    if (this[offset] !in setOf('+', '-')) return false
    val previous = previousNonHorizontalWhitespace(offset) ?: return true
    return previous in setOf('(', ',', '=', '<', '>', '!', '+', '-', '*', '/', '%', '|') ||
        previousTermBefore(offset)?.let { term -> term in unarySignPrecedingTerms } == true
}

private fun String.previousNonHorizontalWhitespace(offset: Int): Char? {
    var index = offset - 1
    while (index >= 0 && this[index].isHorizontalWhitespace()) {
        index--
    }
    return getOrNull(index)
}

private fun String.previousTermBefore(offset: Int): SqlDialectSourceTerm? =
    sqlTokens()
        .takeWhile { token -> token.endOffset <= offset }
        .lastOrNull()
        ?.let { token -> unarySignPrecedingTerms.firstOrNull { term -> token.isTerm(term) } }

private fun Char.isHorizontalWhitespace(): Boolean = this == ' ' || this == '\t'

private fun Char.isComparisonOperatorCharacter(): Boolean = this in setOf('<', '>', '!', '=')

private val unarySignPrecedingTerms =
    setOf(
        SqlDialectSourceTerm.And,
        SqlDialectSourceTerm.By,
        SqlDialectSourceTerm.Else,
        SqlDialectSourceTerm.Or,
        SqlDialectSourceTerm.Select,
        SqlDialectSourceTerm.Set,
        SqlDialectSourceTerm.Then,
        SqlDialectSourceTerm.Values,
        SqlDialectSourceTerm.When,
        SqlDialectSourceTerm.Where,
    )
