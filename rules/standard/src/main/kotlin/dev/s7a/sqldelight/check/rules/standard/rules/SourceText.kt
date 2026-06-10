package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.SourcePosition
import dev.s7a.sqldelight.check.api.SourceRange

internal data class LineInfo(
    val number: Int,
    val startOffset: Int,
    val endOffset: Int,
    val newlineEndOffset: Int,
    val text: String,
)

internal data class SqlToken(
    val text: String,
    val startOffset: Int,
    val endOffset: Int,
)

internal fun String.rangeAtOffsets(
    startOffset: Int,
    endOffset: Int,
): SourceRange =
    SourceRange(
        start = positionAt(startOffset),
        end = positionAt(endOffset),
    )

internal fun String.positionAt(offset: Int): SourcePosition {
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

internal fun String.linesWithRanges(): List<LineInfo> {
    val lines = mutableListOf<LineInfo>()
    var lineNumber = 1
    var lineStart = 0
    var index = 0
    while (index < length) {
        if (this[index] == '\n') {
            lines +=
                LineInfo(
                    number = lineNumber,
                    startOffset = lineStart,
                    endOffset = index,
                    newlineEndOffset = index + 1,
                    text = substring(lineStart, index),
                )
            lineNumber++
            lineStart = index + 1
        }
        index++
    }
    if (lineStart < length || isEmpty()) {
        lines +=
            LineInfo(
                number = lineNumber,
                startOffset = lineStart,
                endOffset = length,
                newlineEndOffset = length,
                text = substring(lineStart, length),
            )
    }
    return lines
}

internal fun String.sqlTokens(): Sequence<SqlToken> =
    sequence {
        var index = 0
        while (index < length) {
            index =
                when {
                    startsWith("--", index) -> skipLineComment(index)
                    startsWith("/*", index) -> skipBlockComment(index)
                    this@sqlTokens[index] == '\'' -> skipQuoted(index, '\'')
                    this@sqlTokens[index] == '"' -> skipQuoted(index, '"')
                    this@sqlTokens[index] == '`' -> skipQuoted(index, '`')
                    this@sqlTokens[index] == '[' -> skipBracketQuoted(index)
                    this@sqlTokens[index].isIdentifierStart() -> {
                        val start = index
                        index++
                        while (index < length && this@sqlTokens[index].isIdentifierPart()) {
                            index++
                        }
                        yield(SqlToken(text = substring(start, index), startOffset = start, endOffset = index))
                        index
                    }
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
            if (quote != '`' && index + 1 < length && this[index + 1] == quote) {
                index += 2
            } else {
                return index + 1
            }
        } else {
            index++
        }
    }
    return length
}

private fun String.skipBracketQuoted(start: Int): Int {
    var index = start + 1
    while (index < length) {
        if (this[index] == ']') {
            if (index + 1 < length && this[index + 1] == ']') {
                index += 2
            } else {
                return index + 1
            }
        } else {
            index++
        }
    }
    return length
}

private fun Char.isIdentifierStart(): Boolean = this == '_' || isLetter()

private fun Char.isIdentifierPart(): Boolean = this == '_' || isLetterOrDigit()
