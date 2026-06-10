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

internal data class SqlCharacter(
    val value: Char,
    val offset: Int,
)

internal data class LineComment(
    val startOffset: Int,
    val endOffset: Int,
)

internal data class BlockComment(
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

internal fun String.identifierTokenAt(offset: Int): SqlToken? {
    if (offset !in indices || !this[offset].isIdentifierStart()) return null
    var end = offset + 1
    while (end < length && this[end].isIdentifierPart()) {
        end++
    }
    return SqlToken(text = substring(offset, end), startOffset = offset, endOffset = end)
}

internal fun String.sqlCharacters(): Sequence<SqlCharacter> =
    sequence {
        var index = 0
        while (index < length) {
            index =
                when {
                    startsWith("--", index) -> skipLineComment(index)
                    startsWith("/*", index) -> skipBlockComment(index)
                    this@sqlCharacters[index] == '\'' -> skipQuoted(index, '\'')
                    this@sqlCharacters[index] == '"' -> skipQuoted(index, '"')
                    this@sqlCharacters[index] == '`' -> skipQuoted(index, '`')
                    this@sqlCharacters[index] == '[' -> skipBracketQuoted(index)
                    else -> {
                        yield(SqlCharacter(value = this@sqlCharacters[index], offset = index))
                        index + 1
                    }
                }
        }
    }

internal fun String.statementEndAfter(offset: Int): Int =
    sqlCharacters()
        .firstOrNull { character -> character.offset >= offset && character.value == ';' }
        ?.offset
        ?: length

internal fun String.lineComments(): Sequence<LineComment> =
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

internal fun String.blockComments(): Sequence<BlockComment> =
    sequence {
        var index = 0
        while (index < length) {
            index =
                when {
                    startsWith("--", index) -> skipLineComment(index)
                    startsWith("/*", index) -> {
                        val end = skipBlockComment(index)
                        yield(BlockComment(startOffset = index, endOffset = end))
                        end
                    }
                    this@blockComments[index] == '\'' -> skipQuoted(index, '\'')
                    this@blockComments[index] == '"' -> skipQuoted(index, '"')
                    this@blockComments[index] == '`' -> skipQuoted(index, '`')
                    this@blockComments[index] == '[' -> skipBracketQuoted(index)
                    else -> index + 1
                }
        }
    }

internal fun String.nextNonHorizontalWhitespace(offset: Int): Char? {
    val index = nextNonHorizontalWhitespaceOffset(offset) ?: return null
    return this[index]
}

internal fun String.nextNonHorizontalWhitespaceOffset(offset: Int): Int? {
    var index = offset
    while (index < length && (this[index] == ' ' || this[index] == '\t')) {
        index++
    }
    return if (index < length) index else null
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
