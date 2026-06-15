package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatterns
import dev.s7a.sqldelight.check.api.SqlDialectSourceTerm

internal data class MappedTypeName(
    val text: String,
    val startOffset: Int,
    val endOffset: Int,
)

internal fun String.mappedTypeNames(sourcePatterns: SqlDialectSourcePatterns): Sequence<MappedTypeName> =
    sequence {
        val tokens = sqlTokens().toList()
        tokens.forEachIndexed { index, token ->
            if (!token.isTerm(SqlDialectSourceTerm.As)) return@forEachIndexed
            val previous = tokens.getOrNull(index - 1) ?: return@forEachIndexed
            val next = tokens.getOrNull(index + 1) ?: return@forEachIndexed
            if (!previous.matches(sourcePatterns, SqlDialectSourcePatternRole.SqlDelightMappableStorageTypeName)) {
                return@forEachIndexed
            }
            if (next.matches(sourcePatterns, SqlDialectSourcePatternRole.ColumnConstraintStart)) return@forEachIndexed

            val endOffset = mappedTypeEndOffset(next)
            yield(
                MappedTypeName(
                    text = substring(next.startOffset, endOffset),
                    startOffset = next.startOffset,
                    endOffset = endOffset,
                ),
            )
        }
    }

private fun String.mappedTypeEndOffset(firstToken: SqlToken): Int {
    var endOffset = firstToken.endOffset
    while (endOffset < length && this[endOffset] == '.') {
        val next = identifierTokenAt(endOffset + 1) ?: break
        endOffset = next.endOffset
    }
    return endOffset
}
