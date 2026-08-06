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
    var lo = 0
    var hi = size - 1
    while (lo <= hi) {
        val mid = (lo + hi).ushr(1)
        val midStart = this[mid].startOffset
        when {
            midStart == offset -> return this[mid]
            midStart < offset -> lo = mid + 1
            else -> hi = mid - 1
        }
    }
    return getOrNull(lo - 1)
}

internal fun String.hasNewlineBetween(startOffset: Int, endOffset: Int): Boolean {
    val idx = indexOf('\n', startOffset)
    return idx != -1 && idx < endOffset
}

/**
 * Returns true when [offset] is the first non-whitespace character on its line.
 * Avoids building linesWithRanges() and lineContaining() for this common pattern.
 */
internal fun String.isFirstNonWhitespaceAt(offset: Int): Boolean {
    var i = offset - 1
    while (i >= 0) {
        val c = this[i]
        if (c == '\n') return true
        if (c != ' ' && c != '\t') return false
        i--
    }
    return true
}
