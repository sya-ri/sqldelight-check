@file:OptIn(InternalSqldelightCheckApi::class)

package dev.s7a.sqldelight.check.core

import dev.s7a.sqldelight.check.api.DatabaseContext
import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.InternalSqldelightCheckApi
import dev.s7a.sqldelight.check.api.QualifiedRuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SourceFile
import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

internal fun SourceFile.suppressionReasonDiagnostics(
    ruleId: QualifiedRuleId,
    severity: Severity,
    database: DatabaseContext,
): List<Diagnostic> =
    content.disableDirectivesWithoutReason()
        .map { directive ->
            Diagnostic(
                ruleId = ruleId,
                severity = severity,
                message = "sqldelight-check disable directives should include a reason after '--'.",
                file = this,
                range = content.rangeAtOffsets(directive.startOffset, directive.endOffset),
                database = database,
            )
        }

private data class DisableDirectiveWithoutReason(
    val startOffset: Int,
    val endOffset: Int,
)

private data class LineComment(
    val startOffset: Int,
    val endOffset: Int,
)

private fun String.disableDirectivesWithoutReason(): List<DisableDirectiveWithoutReason> =
    lineComments()
        .mapNotNull { comment -> disableDirectiveWithoutReason(comment) }
        .toList()

private fun String.disableDirectiveWithoutReason(comment: LineComment): DisableDirectiveWithoutReason? {
    val text = substring(comment.startOffset, comment.endOffset).removeSuffix("\n")
    val body = text.removePrefix("--").trimStart()
    if (!body.startsWith("sqldelight-check-")) return null
    val withoutPrefix = body.removePrefix("sqldelight-check-")
    val command = withoutPrefix.takeWhile { character -> !character.isWhitespace() }
    if (command !in disableDirectiveCommands) return null
    val payload = withoutPrefix.drop(command.length)
    if (payload.hasDirectiveReason()) return null
    return DisableDirectiveWithoutReason(startOffset = comment.startOffset, endOffset = comment.endOffset)
}

private fun String.hasDirectiveReason(): Boolean {
    val delimiter = indexOf(" --")
    if (delimiter == -1) return false
    return drop(delimiter + 3).isNotBlank()
}

private fun String.lineComments(): Sequence<LineComment> =
    sequence {
        var index = 0
        while (index < length) {
            index =
                when {
                    startsWith("--", index) -> {
                        val end = skipLineComment(index)
                        yield(LineComment(startOffset = index, endOffset = end))
                        end
                    }
                    startsWith("/*", index) -> skipBlockComment(index)
                    this@lineComments[index] == '\'' -> skipQuoted(index, '\'')
                    this@lineComments[index] == '"' -> skipQuoted(index, '"')
                    this@lineComments[index] == '`' -> skipQuoted(index, '`')
                    this@lineComments[index] == '[' -> skipBracketQuoted(index)
                    else -> index + 1
                }
        }
    }

private fun String.skipLineComment(start: Int): Int {
    val newline = indexOf('\n', startIndex = start + 2)
    return if (newline == -1) length else newline + 1
}

private fun String.skipBlockComment(start: Int): Int {
    val end = indexOf("*/", startIndex = start + 2)
    return if (end == -1) length else end + 2
}

private fun String.skipQuoted(
    start: Int,
    quote: Char,
): Int {
    var index = start + 1
    while (index < length) {
        if (this[index] == quote) {
            if (index + 1 < length && this[index + 1] == quote) {
                index += 2
            } else {
                return index + 1
            }
        } else {
            index += 1
        }
    }
    return length
}

private fun String.skipBracketQuoted(start: Int): Int {
    val end = indexOf(']', startIndex = start + 1)
    return if (end == -1) length else end + 1
}

private val disableDirectiveCommands = setOf("disable", "disable-next-line", "disable-file")
