package dev.s7a.sqldelight.check.rules.hsql.rules

import dev.s7a.sqldelight.check.api.SourcePosition
import dev.s7a.sqldelight.check.api.SourceRange

internal fun String.rangeAtOffsets(
    startOffset: Int,
    endOffset: Int,
): SourceRange =
    SourceRange(
        start = positionAt(startOffset),
        end = positionAt(endOffset),
    )

private fun String.positionAt(offset: Int): SourcePosition {
    val boundedOffset = offset.coerceIn(0, length)
    var line = 1
    var lineStart = 0
    var index = 0
    while (index < boundedOffset) {
        if (this[index] == '\n') {
            line++
            lineStart = index + 1
        }
        index++
    }
    return SourcePosition(line = line, column = boundedOffset - lineStart + 1)
}

internal fun String.maskSqlCommentsAndQuotedText(): String {
    val chars = toCharArray()
    var index = 0
    while (index < chars.size) {
        index =
            when {
                startsWith("--", index) -> maskLineComment(chars, index)
                startsWith("/*", index) -> maskBlockComment(chars, index)
                chars[index] == '\'' -> maskQuoted(chars, index, '\'')
                chars[index] == '"' -> maskQuoted(chars, index, '"')
                else -> index + 1
            }
    }
    return String(chars)
}

private fun String.maskLineComment(
    chars: CharArray,
    start: Int,
): Int {
    val end = indexOf('\n', startIndex = start).let { if (it == -1) length else it }
    for (index in start until end) chars[index] = ' '
    return end
}

private fun String.maskBlockComment(
    chars: CharArray,
    start: Int,
): Int {
    val end = indexOf("*/", startIndex = start + 2).let { if (it == -1) length else it + 2 }
    for (index in start until end) chars[index] = ' '
    return end
}

private fun maskQuoted(
    chars: CharArray,
    start: Int,
    quote: Char,
): Int {
    var index = start + 1
    chars[start] = ' '
    while (index < chars.size) {
        val current = chars[index]
        chars[index] = ' '
        if (current == quote) {
            val next = index + 1
            if (next < chars.size && chars[next] == quote) {
                chars[next] = ' '
                index += 2
            } else {
                return next
            }
        } else {
            index++
        }
    }
    return chars.size
}
