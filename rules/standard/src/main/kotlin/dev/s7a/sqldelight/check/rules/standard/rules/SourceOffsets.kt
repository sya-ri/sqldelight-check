package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.SourcePosition

internal fun SourcePosition.toOffsetIn(content: String): Int {
    val lines = content.linesWithRanges()
    val lineInfo = lines.getOrNull(line - 1) ?: return content.length
    return (lineInfo.startOffset + column - 1).coerceAtMost(lineInfo.endOffset)
}
