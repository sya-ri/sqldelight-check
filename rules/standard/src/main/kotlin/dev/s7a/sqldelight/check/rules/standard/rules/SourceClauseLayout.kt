package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatterns

internal data class ClauseItem(
    val startOffset: Int,
    val endOffset: Int,
)

internal fun String.commaSeparatedClauseItems(
    startOffset: Int,
    endOffset: Int,
    depth: Int,
    parenthesisDepths: IntArray,
): List<ClauseItem> {
    val items = mutableListOf<ClauseItem>()
    var itemStart = startOffset
    sqlCharacters()
        .dropWhile { character -> character.offset < startOffset }
        .takeWhile { character -> character.offset < endOffset }
        .forEach { character ->
            if (character.value == ',' && parenthesisDepths[character.offset] == depth) {
                trimmedClauseItem(itemStart, character.offset)?.let(items::add)
                itemStart = character.offset + 1
            }
        }
    trimmedClauseItem(itemStart, endOffset)?.let(items::add)
    return items
}

private fun String.trimmedClauseItem(
    startOffset: Int,
    endOffset: Int,
): ClauseItem? {
    var start = startOffset
    var end = endOffset
    while (start < end && this[start].isWhitespace()) start++
    while (end > start && this[end - 1].isWhitespace()) end--
    return if (start < end) ClauseItem(startOffset = start, endOffset = end) else null
}

internal fun String.isMultilineItemList(items: List<ClauseItem>): Boolean =
    items.size > 1 && hasNewlineBetween(items.first().startOffset, items.last().endOffset)

internal fun List<LineInfo>.itemStartsAreOnOwnLines(items: List<ClauseItem>): Boolean =
    items.all { item -> lineContaining(item.startOffset)?.firstNonWhitespaceOffset == item.startOffset }

internal fun List<LineInfo>.misplacedItemStarts(items: List<ClauseItem>): List<Int> =
    items
        .filter { item -> lineContaining(item.startOffset)?.firstNonWhitespaceOffset != item.startOffset }
        .map { item -> item.startOffset }

internal fun List<SqlToken>.firstBoundaryOffsetAfterAtDepth(
    content: String,
    startIndex: Int,
    statementEnd: Int,
    depth: Int,
    sourcePatterns: SqlDialectSourcePatterns,
    role: SqlDialectSourcePatternRole,
    parenthesisDepths: IntArray,
): Int =
    firstBoundaryOffsetAfterAtDepth(content, startIndex, statementEnd, depth, sourcePatterns, setOf(role), parenthesisDepths)

internal fun List<SqlToken>.firstBoundaryOffsetAfterAtDepth(
    content: String,
    startIndex: Int,
    statementEnd: Int,
    depth: Int,
    sourcePatterns: SqlDialectSourcePatterns,
    roles: Set<SqlDialectSourcePatternRole>,
    parenthesisDepths: IntArray,
): Int =
    asSequence()
        .drop(startIndex)
        .withIndex()
        .firstOrNull { (relativeIndex, token) ->
            token.startOffset < statementEnd &&
                parenthesisDepths[token.startOffset] == depth &&
                roles.any { role -> sourcePatterns.matches(role, normalizedTextsFrom(startIndex + relativeIndex)) }
        }
        ?.value
        ?.startOffset
        ?: statementEnd
