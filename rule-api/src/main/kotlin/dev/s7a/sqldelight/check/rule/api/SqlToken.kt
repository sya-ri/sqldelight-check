package dev.s7a.sqldelight.check.rule.api

/**
 * SQL-like source token outside comments and quoted text.
 */
public class SqlToken(
    public val text: String,
    public val startOffset: Int,
    public val endOffset: Int,
) {
    /**
     * Lowercase token text for case-insensitive matching.
     */
    public val normalizedText: String = text.lowercase()

    override fun equals(other: Any?): Boolean =
        this === other ||
            other is SqlToken &&
            text == other.text &&
            startOffset == other.startOffset &&
            endOffset == other.endOffset

    override fun hashCode(): Int {
        var result = text.hashCode()
        result = 31 * result + startOffset
        result = 31 * result + endOffset
        return result
    }

    override fun toString(): String =
        "SqlToken(text=$text, startOffset=$startOffset, endOffset=$endOffset)"
}

/**
 * Returns true when this token matches [value] ignoring case.
 */
public fun SqlToken.isKeyword(value: String): Boolean = text.equals(value, ignoreCase = true)

/**
 * Tokenizes SQL-like identifiers, numbers, and semicolons outside comments and quoted text.
 */
public fun String.sqlTokens(hashLineComments: Boolean = false): Sequence<SqlToken> =
    sequence {
        var index = 0
        while (index < length) {
            index =
                when {
                    startsWith("--", index) -> skipLineComment(index)
                    hashLineComments && this@sqlTokens[index] == '#' -> skipLineComment(index)
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
                    this@sqlTokens[index].isDigit() -> {
                        val start = index
                        index++
                        while (index < length && this@sqlTokens[index].isDigit()) {
                            index++
                        }
                        yield(SqlToken(text = substring(start, index), startOffset = start, endOffset = index))
                        index
                    }
                    else -> index + 1
                }
        }
    }

/**
 * Splits a token stream into statements at semicolon tokens.
 */
public fun List<SqlToken>.sqlStatements(): List<List<SqlToken>> {
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
