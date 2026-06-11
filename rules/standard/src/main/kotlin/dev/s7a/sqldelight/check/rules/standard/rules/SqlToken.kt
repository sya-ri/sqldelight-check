package dev.s7a.sqldelight.check.rules.standard.rules

internal data class SqlToken(
    val text: String,
    val startOffset: Int,
    val endOffset: Int,
) {
    val normalizedText: String = text.lowercase()
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
