package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatterns
import dev.s7a.sqldelight.check.api.SqlDialectSourceTerm

internal data class MappedTypeName(
    val text: String,
    val asOffset: Int,
    val startOffset: Int,
    val endOffset: Int,
) {
    val outerName: String = text.substringBefore('<')
    val outerEndOffset: Int = startOffset + outerName.length
}

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

            val endOffset = mappedTypeEndOffset(next.startOffset)
            yield(
                MappedTypeName(
                    text = substring(next.startOffset, endOffset),
                    asOffset = token.startOffset,
                    startOffset = next.startOffset,
                    endOffset = endOffset,
                ),
            )
        }
    }

internal fun List<MappedTypeName>.containsOffset(offset: Int): Boolean =
    any { type -> offset in type.startOffset until type.endOffset }

internal fun List<MappedTypeName>.hasBindingStartAt(offset: Int): Boolean =
    any { type -> type.asOffset == offset }

internal fun List<MappedTypeName>.hasTypeOnSameLine(
    content: String,
    offset: Int,
): Boolean =
    any { type ->
        offset >= type.endOffset && content.lineStartOffset(type.startOffset) == content.lineStartOffset(offset)
    }

private fun String.mappedTypeEndOffset(startOffset: Int): Int {
    var index = startOffset
    var angleDepth = 0
    while (index < length) {
        val character = this[index]
        when {
            character == '<' -> {
                angleDepth++
                index++
            }
            character == '>' && angleDepth > 0 -> {
                angleDepth--
                index++
            }
            angleDepth == 0 && character.isMappedTypeBoundary() -> return index
            else -> index++
        }
    }
    return index
}

private fun Char.isMappedTypeBoundary(): Boolean =
    isWhitespace() || this == ',' || this == ')' || this == ';'

internal fun String.lineStartOffset(offset: Int): Int =
    lastIndexOf('\n', startIndex = (offset - 1).coerceAtLeast(0)).let { index -> if (index == -1) 0 else index + 1 }
