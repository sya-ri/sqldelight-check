package dev.s7a.sqldelight.check.rules.sqlite.rules

import dev.s7a.sqldelight.check.api.DialectCapabilities
import dev.s7a.sqldelight.check.api.SourcePosition
import dev.s7a.sqldelight.check.api.SourceRange
import dev.s7a.sqldelight.check.rule.api.RuleContext

internal fun RuleContext.isSQLite(): Boolean = DialectCapabilities.SQLite in database.dialect.capabilities

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
                chars[index] == '`' -> maskQuoted(chars, index, '`')
                chars[index] == '[' -> maskBracketQuoted(chars, index)
                else -> index + 1
            }
    }
    return String(chars)
}

internal data class SqlToken(
    val text: String,
    val startOffset: Int,
    val endOffset: Int,
) {
    val normalizedText: String = text.lowercase()
}

internal fun SqlToken.isKeyword(value: String): Boolean = text.equals(value, ignoreCase = true)

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
                    this@sqlTokens[index] == ';' -> {
                        yield(SqlToken(text = ";", startOffset = index, endOffset = index + 1))
                        index + 1
                    }
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

internal fun List<SqlToken>.sqlStatements(): List<List<SqlToken>> {
    val statements = mutableListOf<List<SqlToken>>()
    val current = mutableListOf<SqlToken>()
    for (token in this) {
        current += token
        if (token.text == ";") {
            statements += current.toList()
            current.clear()
        }
    }
    if (current.isNotEmpty()) statements += current.toList()
    return statements
}

private fun String.maskLineComment(
    chars: CharArray,
    start: Int,
): Int {
    val end = indexOf('\n', startIndex = start).let { if (it == -1) length else it }
    for (index in start until end) chars[index] = ' '
    return end
}

private fun String.skipLineComment(start: Int): Int =
    indexOf('\n', startIndex = start).let { if (it == -1) length else it }

private fun String.skipBlockComment(start: Int): Int =
    indexOf("*/", startIndex = start + 2).let { if (it == -1) length else it + 2 }

private fun String.skipQuoted(
    start: Int,
    quote: Char,
): Int {
    var index = start + 1
    while (index < length) {
        if (this[index] == quote) {
            val next = index + 1
            if (next < length && this[next] == quote) {
                index += 2
            } else {
                return next
            }
        } else {
            index++
        }
    }
    return length
}

private fun String.skipBracketQuoted(start: Int): Int =
    indexOf(']', startIndex = start + 1).let { if (it == -1) length else it + 1 }

private fun Char.isIdentifierStart(): Boolean = isLetter() || this == '_'

private fun Char.isIdentifierPart(): Boolean = isLetterOrDigit() || this == '_' || this == '$'

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

private fun String.maskBracketQuoted(
    chars: CharArray,
    start: Int,
): Int {
    val end = indexOf(']', startIndex = start + 1).let { if (it == -1) length else it + 1 }
    for (index in start until end) chars[index] = ' '
    return end
}
