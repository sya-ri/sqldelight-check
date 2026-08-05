package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.AliasBoundary
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatterns
import dev.s7a.sqldelight.check.api.SqlDialectSourceTerm

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
        val parenthesisDepths = computeParenthesisDepths()
        tokens.forEachIndexed { index, token ->
            if (!token.isTerm(SqlDialectSourceTerm.Select)) return@forEachIndexed
            val selectDepth = parenthesisDepths[token.startOffset]
            val statementEnd = statementEndAfter(token.startOffset)
            val fromToken =
                tokens
                    .drop(index + 1)
                    .firstOrNull { candidate ->
                        candidate.startOffset < statementEnd &&
                            parenthesisDepths[candidate.startOffset] == selectDepth &&
                            candidate.isTerm(SqlDialectSourceTerm.From)
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

internal fun String.resultColumnAliases(
    sourcePatterns: SqlDialectSourcePatterns = SqlDialectSourcePatterns.SourceScannerDefault,
): List<ResultColumnAlias> {
    val aliases = mutableListOf<ResultColumnAlias>()
    selectFromRanges().forEach { select ->
        selectTargets(select.selectEndOffset, select.fromStartOffset, select.depth).forEach { target ->
            target.aliasIn(this, sourcePatterns)?.let { alias ->
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
    var currentDepth = 0
    sqlCharacters()
        .takeWhile { character -> character.offset < endOffset }
        .forEach { character ->
            when {
                character.value == '(' -> currentDepth++
                character.value == ')' -> if (currentDepth > 0) currentDepth--
                character.value == ',' && character.offset >= startOffset && currentDepth == depth -> {
                    targets += AliasSelectTarget(targetStart, character.offset).trimmedIn(this)
                    targetStart = character.offset + 1
                }
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

private fun AliasSelectTarget.aliasIn(
    content: String,
    sourcePatterns: SqlDialectSourcePatterns,
): TargetAlias? {
    val targetText = content.substring(startOffset, endOffset)
    val tokens = targetText.sqlTokens().toList()
    if (tokens.size < 2) return null

    val last = tokens.last().toSourceToken(startOffset)
    val previous = tokens[tokens.lastIndex - 1]
    if (previous.isTerm(SqlDialectSourceTerm.As)) return TargetAlias(token = last, usesAs = true)
    if (last.matches(sourcePatterns, AliasBoundary)) return null
    if (previous.endOffset >= tokens.last().startOffset) return null
    return TargetAlias(token = last, usesAs = false)
}

private fun SqlToken.toSourceToken(baseOffset: Int): SqlToken =
    SqlToken(
        text = text,
        startOffset = baseOffset + startOffset,
        endOffset = baseOffset + endOffset,
    )
