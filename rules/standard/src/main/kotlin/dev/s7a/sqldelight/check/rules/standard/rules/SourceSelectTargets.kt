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
    val clauses = mutableListOf<SourceSelectClauseTargets>()
    selectFromRanges().forEach { select ->
        val targets = selectTargetsWithCommas(select.selectEndOffset, select.fromStartOffset, select.depth)
        if (targets.isNotEmpty()) {
            clauses +=
                SourceSelectClauseTargets(
                    select = select.select,
                    listStartOffset = select.selectEndOffset,
                    listEndOffset = select.fromStartOffset,
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
    var currentDepth = 0
    sqlCharacters()
        .takeWhile { character -> character.offset < endOffset }
        .forEach { character ->
            when {
                character.value == '(' -> currentDepth++
                character.value == ')' -> if (currentDepth > 0) currentDepth--
                character.value == ',' && character.offset >= startOffset && currentDepth == depth -> {
                    trimmedSelectTarget(targetStart, character.offset, character.offset)?.let(targets::add)
                    targetStart = character.offset + 1
                }
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
