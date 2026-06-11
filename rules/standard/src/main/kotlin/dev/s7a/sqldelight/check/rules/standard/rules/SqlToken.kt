package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatterns
import dev.s7a.sqldelight.check.api.SqlDialectSourceTerm

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

internal fun SqlToken.isTerm(term: SqlDialectSourceTerm): Boolean =
    normalizedText == term.normalizedText

internal fun SqlToken.isKeyword(value: String): Boolean = text.equals(value, ignoreCase = true)

internal fun SqlToken.matches(
    sourcePatterns: SqlDialectSourcePatterns,
    role: SqlDialectSourcePatternRole,
): Boolean =
    sourcePatterns.matches(role, listOf(normalizedText))

internal fun List<SqlToken>.containsKeywordPair(
    first: String,
    second: String,
): Boolean =
    asSequence()
        .zipWithNext()
        .any { (left, right) -> left.isKeyword(first) && right.isKeyword(second) }

internal fun List<SqlToken>.containsTermPair(
    first: SqlDialectSourceTerm,
    second: SqlDialectSourceTerm,
): Boolean =
    asSequence()
        .zipWithNext()
        .any { (left, right) -> left.isTerm(first) && right.isTerm(second) }

internal fun List<SqlToken>.firstTermAfter(
    startIndex: Int,
    statementEnd: Int,
    term: SqlDialectSourceTerm,
): SqlToken? =
    asSequence()
        .drop(startIndex)
        .firstOrNull { token -> token.startOffset < statementEnd && token.isTerm(term) }

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

internal fun List<SqlToken>.firstBoundaryOffsetAfter(
    startIndex: Int,
    statementEnd: Int,
    sourcePatterns: SqlDialectSourcePatterns,
    role: SqlDialectSourcePatternRole,
): Int =
    asSequence()
        .drop(startIndex)
        .withIndex()
        .firstOrNull { (relativeIndex, token) ->
            token.startOffset < statementEnd &&
                sourcePatterns.matches(role, normalizedTextsFrom(startIndex + relativeIndex))
        }?.value?.startOffset
        ?: statementEnd

internal fun List<SqlToken>.lastTermBefore(
    offset: Int,
    terms: Set<SqlDialectSourceTerm>,
): SqlDialectSourceTerm? =
    asSequence()
        .takeWhile { token -> token.startOffset < offset }
        .mapNotNull { token -> terms.firstOrNull { term -> token.isTerm(term) } }
        .lastOrNull()

internal fun List<SqlToken>.lastKeywordBefore(
    offset: Int,
    keywords: Set<String>,
): String? =
    asSequence()
        .takeWhile { token -> token.startOffset < offset }
        .map { token -> token.normalizedText }
        .lastOrNull { token -> token in keywords }

internal fun List<SqlToken>.normalizedTextsFrom(index: Int): List<String> =
    drop(index).map { token -> token.normalizedText }
