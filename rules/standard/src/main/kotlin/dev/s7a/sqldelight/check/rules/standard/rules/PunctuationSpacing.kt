package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.Fix
import dev.s7a.sqldelight.check.api.FixSafety
import dev.s7a.sqldelight.check.api.TextEdit
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

internal data class ComparisonOperator(
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

            val range = rangeAtOffsets(whitespaceStart, character.offset)
            reporter.report(
                Diagnostic(
                    ruleId = rule.id,
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

internal fun String.comparisonOperatorAt(offset: Int): ComparisonOperator? {
    if (offset >= length) return null
    return when (this[offset]) {
        '=' ->
            when {
                offset > 0 && this[offset - 1].isComparisonOperatorCharacter() -> null
                offset + 1 < length && this[offset + 1].isComparisonOperatorCharacter() -> null
                else -> ComparisonOperator(startOffset = offset, endOffset = offset + 1)
            }
        '!' ->
            if (offset + 1 < length && this[offset + 1] == '=') {
                ComparisonOperator(startOffset = offset, endOffset = offset + 2)
            } else {
                null
            }
        '<' ->
            when {
                offset + 1 < length && this[offset + 1] in setOf('=', '>') ->
                    ComparisonOperator(startOffset = offset, endOffset = offset + 2)
                offset + 1 < length && this[offset + 1].isComparisonOperatorCharacter() -> null
                else -> ComparisonOperator(startOffset = offset, endOffset = offset + 1)
            }
        '>' ->
            when {
                offset > 0 && this[offset - 1].isComparisonOperatorCharacter() -> null
                offset + 1 < length && this[offset + 1] == '=' ->
                    ComparisonOperator(startOffset = offset, endOffset = offset + 2)
                offset + 1 < length && this[offset + 1].isComparisonOperatorCharacter() -> null
                else -> ComparisonOperator(startOffset = offset, endOffset = offset + 1)
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

private fun Char.isHorizontalWhitespace(): Boolean = this == ' ' || this == '\t'

private fun Char.isComparisonOperatorCharacter(): Boolean = this in setOf('<', '>', '!', '=')
