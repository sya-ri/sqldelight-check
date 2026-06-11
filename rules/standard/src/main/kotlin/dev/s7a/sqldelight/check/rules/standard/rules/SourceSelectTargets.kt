package dev.s7a.sqldelight.check.rules.standard.rules

internal data class SourceSelectClauseTargets(
    val select: SqlToken,
    val listStartOffset: Int,
    val listEndOffset: Int,
    val targets: List<SourceSelectTarget>,
)

internal data class SourceSelectTarget(
    val startOffset: Int,
    val endOffset: Int,
    val commaOffset: Int?,
)

internal fun String.sourceSelectClauseTargets(): List<SourceSelectClauseTargets> {
    val tokens = sqlTokens().toList()
    val clauses = mutableListOf<SourceSelectClauseTargets>()
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
        val targets = selectTargetsWithCommas(token.endOffset, fromToken.startOffset, selectDepth)
        if (targets.isNotEmpty()) {
            clauses +=
                SourceSelectClauseTargets(
                    select = token,
                    listStartOffset = token.endOffset,
                    listEndOffset = fromToken.startOffset,
                    targets = targets,
                )
        }
    }
    return clauses
}

private fun String.selectTargetsWithCommas(
    startOffset: Int,
    endOffset: Int,
    depth: Int,
): List<SourceSelectTarget> {
    val targets = mutableListOf<SourceSelectTarget>()
    var targetStart = startOffset
    sqlCharacters()
        .dropWhile { character -> character.offset < startOffset }
        .takeWhile { character -> character.offset < endOffset }
        .forEach { character ->
            if (character.value == ',' && sqlParenthesisDepthAt(character.offset) == depth) {
                trimmedSelectTarget(targetStart, character.offset, character.offset)?.let(targets::add)
                targetStart = character.offset + 1
            }
        }
    trimmedSelectTarget(targetStart, endOffset, commaOffset = null)?.let(targets::add)
    return targets
}

private fun String.trimmedSelectTarget(
    startOffset: Int,
    endOffset: Int,
    commaOffset: Int?,
): SourceSelectTarget? {
    var start = startOffset
    var end = endOffset
    while (start < end && this[start].isWhitespace()) start++
    while (end > start && this[end - 1].isWhitespace()) end--
    return if (start < end) SourceSelectTarget(startOffset = start, endOffset = end, commaOffset = commaOffset) else null
}
