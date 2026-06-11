package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.SourcePosition
import dev.s7a.sqldelight.check.api.SourceRange

internal data class LineInfo(
    val number: Int,
    val startOffset: Int,
    val endOffset: Int,
    val newlineEndOffset: Int,
    val text: String,
) {
    val firstNonWhitespaceOffset: Int?
        get() {
            val index = text.indexOfFirst { character -> character != ' ' && character != '\t' }
            return if (index == -1) null else startOffset + index
        }
}

internal data class SqlToken(
    val text: String,
    val startOffset: Int,
    val endOffset: Int,
) {
    val normalizedText: String = text.lowercase()
}

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

internal fun List<LineInfo>.lineContaining(offset: Int): LineInfo? =
    lastOrNull { line -> line.startOffset <= offset }

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

internal fun String.sqlParenthesisDepthAt(offset: Int): Int {
    var depth = 0
    sqlCharacters()
        .takeWhile { character -> character.offset < offset }
        .forEach { character ->
            when (character.value) {
                '(' -> depth++
                ')' -> if (depth > 0) depth--
            }
        }
    return depth
}

internal fun String.previousSqlCharacterBefore(offset: Int): SqlCharacter? =
    sqlCharacters()
        .takeWhile { character -> character.offset < offset }
        .filterNot { character -> character.value.isWhitespace() }
        .lastOrNull()

internal fun String.nextSqlCharacterAfter(offset: Int): SqlCharacter? =
    sqlCharacters()
        .dropWhile { character -> character.offset < offset }
        .firstOrNull { character -> !character.value.isWhitespace() }

internal fun String.matchingClosingParenthesisOffset(openOffset: Int): Int? {
    if (getOrNull(openOffset) != '(') return null

    var depth = 0
    sqlCharacters()
        .dropWhile { character -> character.offset < openOffset }
        .forEach { character ->
            when (character.value) {
                '(' -> depth++
                ')' -> {
                    depth--
                    if (depth == 0) return character.offset
                }
            }
        }
    return null
}

internal fun SqlToken.isKeyword(value: String): Boolean = text.equals(value, ignoreCase = true)

internal fun List<SqlToken>.containsKeywordPair(
    first: String,
    second: String,
): Boolean =
    asSequence()
        .zipWithNext()
        .any { (left, right) -> left.isKeyword(first) && right.isKeyword(second) }

internal fun List<SqlToken>.firstKeywordAfter(
    startIndex: Int,
    statementEnd: Int,
    keyword: String,
): SqlToken? =
    asSequence()
        .drop(startIndex)
        .firstOrNull { token -> token.startOffset < statementEnd && token.isKeyword(keyword) }

internal fun List<SqlToken>.firstBoundaryOffsetAfter(
    startIndex: Int,
    statementEnd: Int,
    boundaryKeywords: Set<String>,
): Int =
    asSequence()
        .drop(startIndex)
        .firstOrNull { token -> token.startOffset < statementEnd && token.normalizedText in boundaryKeywords }
        ?.startOffset
        ?: statementEnd

internal fun List<SqlToken>.lastKeywordBefore(
    offset: Int,
    keywords: Set<String>,
): String? =
    asSequence()
        .takeWhile { token -> token.startOffset < offset }
        .map { token -> token.normalizedText }
        .filter { token -> token in keywords }
        .lastOrNull()

internal fun String.previousNonWhitespaceOffset(
    offset: Int,
    expected: Char,
): Int? {
    var index = offset - 1
    while (index >= 0 && this[index].isWhitespace()) {
        index--
    }
    return if (getOrNull(index) == expected) index else null
}

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
