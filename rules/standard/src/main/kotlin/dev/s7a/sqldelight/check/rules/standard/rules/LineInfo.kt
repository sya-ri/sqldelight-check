package dev.s7a.sqldelight.check.rules.standard.rules

internal data class LineInfo(
    val number: Int,
    val startOffset: Int,
    val endOffset: Int,
    val newlineEndOffset: Int,
    val text: String,
) {
    val firstNonWhitespaceOffset: Int?
        get() {
            val index = text.indexOfFirst { character -> character != ' ' && character != '\t' }
            return if (index == -1) null else startOffset + index
        }
}

internal fun String.linesWithRanges(): List<LineInfo> {
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

internal fun List<LineInfo>.lineContaining(offset: Int): LineInfo? =
    lastOrNull { line -> line.startOffset <= offset }
