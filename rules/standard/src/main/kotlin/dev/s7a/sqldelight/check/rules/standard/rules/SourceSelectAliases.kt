package dev.s7a.sqldelight.check.rules.standard.rules

internal data class ResultColumnAlias(
    val selectStartOffset: Int,
    val targetStartOffset: Int,
    val targetEndOffset: Int,
    val token: SqlToken,
    val usesAs: Boolean,
)

internal data class SelectFromRange(
    val select: SqlToken,
    val selectStartOffset: Int,
    val selectEndOffset: Int,
    val fromStartOffset: Int,
    val depth: Int,
)

internal fun String.selectFromRanges(): Sequence<SelectFromRange> =
    sequence {
        val tokens = sqlTokens().toList()
        tokens.forEachIndexed { index, token ->
            if (!token.isKeyword("select")) return@forEachIndexed
            val selectDepth = sqlParenthesisDepthAt(token.startOffset)
            val statementEnd = statementEndAfter(token.startOffset)
            val fromToken =
                tokens
                    .drop(index + 1)
                    .firstOrNull { candidate ->
                        candidate.startOffset < statementEnd &&
                            sqlParenthesisDepthAt(candidate.startOffset) == selectDepth &&
                            candidate.isKeyword("from")
                    } ?: return@forEachIndexed
            yield(
                SelectFromRange(
                    select = token,
                    selectStartOffset = token.startOffset,
                    selectEndOffset = token.endOffset,
                    fromStartOffset = fromToken.startOffset,
                    depth = selectDepth,
                ),
            )
        }
    }

internal fun String.resultColumnAliases(): List<ResultColumnAlias> {
    val aliases = mutableListOf<ResultColumnAlias>()
    selectFromRanges().forEach { select ->
        selectTargets(select.selectEndOffset, select.fromStartOffset, select.depth).forEach { target ->
            target.aliasIn(this)?.let { alias ->
                aliases +=
                    ResultColumnAlias(
                        selectStartOffset = select.selectStartOffset,
                        targetStartOffset = target.startOffset,
                        targetEndOffset = target.endOffset,
                        token = alias.token,
                        usesAs = alias.usesAs,
                    )
            }
        }
    }
    return aliases
}

private data class AliasSelectTarget(
    val startOffset: Int,
    val endOffset: Int,
)

private data class TargetAlias(
    val token: SqlToken,
    val usesAs: Boolean,
)

private fun String.selectTargets(
    startOffset: Int,
    endOffset: Int,
    depth: Int,
): List<AliasSelectTarget> {
    val targets = mutableListOf<AliasSelectTarget>()
    var targetStart = startOffset
    sqlCharacters()
        .dropWhile { character -> character.offset < startOffset }
        .takeWhile { character -> character.offset < endOffset }
        .forEach { character ->
            if (character.value == ',' && sqlParenthesisDepthAt(character.offset) == depth) {
                targets += AliasSelectTarget(targetStart, character.offset).trimmedIn(this)
                targetStart = character.offset + 1
            }
        }
    targets += AliasSelectTarget(targetStart, endOffset).trimmedIn(this)
    return targets.filter { target -> target.startOffset < target.endOffset }
}

private fun AliasSelectTarget.trimmedIn(content: String): AliasSelectTarget {
    var start = startOffset
    var end = endOffset
    while (start < end && content[start].isWhitespace()) start++
    while (end > start && content[end - 1].isWhitespace()) end--
    return AliasSelectTarget(start, end)
}

private fun AliasSelectTarget.aliasIn(content: String): TargetAlias? {
    val targetText = content.substring(startOffset, endOffset)
    val tokens = targetText.sqlTokens().toList()
    if (tokens.size < 2) return null

    val last = tokens.last().toSourceToken(startOffset)
    val previous = tokens[tokens.lastIndex - 1]
    if (previous.isKeyword("as")) return TargetAlias(token = last, usesAs = true)
    if (last.normalizedText in columnAliasBoundaryKeywords) return null
    if (previous.endOffset >= tokens.last().startOffset) return null
    return TargetAlias(token = last, usesAs = false)
}

private fun SqlToken.toSourceToken(baseOffset: Int): SqlToken =
    SqlToken(
        text = text,
        startOffset = baseOffset + startOffset,
        endOffset = baseOffset + endOffset,
    )

private val columnAliasBoundaryKeywords =
    setOf(
        "case",
        "cast",
        "coalesce",
        "count",
        "else",
        "end",
        "false",
        "filter",
        "from",
        "null",
        "over",
        "then",
        "true",
        "when",
    )
