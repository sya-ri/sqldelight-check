package dev.s7a.sqldelight.check.rules.standard.rules

internal data class ClauseItem(
    val startOffset: Int,
    val endOffset: Int,
)

internal fun String.commaSeparatedClauseItems(
    startOffset: Int,
    endOffset: Int,
    depth: Int,
): List<ClauseItem> {
    val items = mutableListOf<ClauseItem>()
    var itemStart = startOffset
    sqlCharacters()
        .dropWhile { character -> character.offset < startOffset }
        .takeWhile { character -> character.offset < endOffset }
        .forEach { character ->
            if (character.value == ',' && sqlParenthesisDepthAt(character.offset) == depth) {
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
    items.size > 1 && substring(items.first().startOffset, items.last().endOffset).contains('\n')

internal fun List<LineInfo>.itemStartsAreOnOwnLines(items: List<ClauseItem>): Boolean =
    items.all { item -> lineContaining(item.startOffset)?.firstNonWhitespaceOffset == item.startOffset }

internal fun List<SqlToken>.firstBoundaryOffsetAfterAtDepth(
    content: String,
    startIndex: Int,
    statementEnd: Int,
    depth: Int,
    boundaryKeywords: Set<String>,
): Int =
    asSequence()
        .drop(startIndex)
        .firstOrNull { token ->
            token.startOffset < statementEnd &&
                content.sqlParenthesisDepthAt(token.startOffset) == depth &&
                token.normalizedText in boundaryKeywords
        }
        ?.startOffset
        ?: statementEnd
