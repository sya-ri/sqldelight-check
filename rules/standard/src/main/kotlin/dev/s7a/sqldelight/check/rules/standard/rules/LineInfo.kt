package dev.s7a.sqldelight.check.rules.standard.rules

import java.util.WeakHashMap

internal data class LineInfo(
    val number: Int,
    val startOffset: Int,
    val endOffset: Int,
    val newlineEndOffset: Int,
    val text: String,
) {
    val firstNonWhitespaceOffset: Int? = run {
        val index = text.indexOfFirst { character -> character != ' ' && character != '\t' }
        if (index == -1) null else startOffset + index
    }
}

private val linesWithRangesCache =
    ThreadLocal.withInitial<WeakHashMap<String, List<LineInfo>>> { WeakHashMap() }

internal fun String.linesWithRanges(): List<LineInfo> =
    linesWithRangesCache.get().getOrPut(this) { buildLinesWithRanges() }

private fun String.buildLinesWithRanges(): List<LineInfo> {
    val lines = mutableListOf<LineInfo>()
    var lineNumber = 1
    var lineStart = 0
    var index = 0
    while (index < length) {
        if (this[index] == '\n') {
            lines +=
                LineInfo(
                    number = lineNumber,
                    startOffset = lineStart,
                    endOffset = index,
                    newlineEndOffset = index + 1,
                    text = substring(lineStart, index),
                )
            lineNumber++
            lineStart = index + 1
        }
        index++
    }
    if (lineStart < length || isEmpty()) {
        lines +=
            LineInfo(
                number = lineNumber,
                startOffset = lineStart,
                endOffset = length,
                newlineEndOffset = length,
                text = substring(lineStart, length),
            )
    }
    return lines
}

internal fun List<LineInfo>.lineContaining(offset: Int): LineInfo? {
    val idx = binarySearchBy(offset) { it.startOffset }
    return if (idx >= 0) this[idx] else getOrNull(-idx - 2)
}

internal fun String.hasNewlineBetween(startOffset: Int, endOffset: Int): Boolean {
    val idx = indexOf('\n', startOffset)
    return idx != -1 && idx < endOffset
}
