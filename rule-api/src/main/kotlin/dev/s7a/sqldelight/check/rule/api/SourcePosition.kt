package dev.s7a.sqldelight.check.rule.api

import dev.s7a.sqldelight.check.api.SourcePosition

/**
 * Converts a source offset into a 1-based line/column position.
 */
public fun String.positionAt(offset: Int): SourcePosition {
    val boundedOffset = offset.coerceIn(0, length)
    var line = 1
    var lineStart = 0
    var index = 0
    while (index < boundedOffset) {
        if (this[index] == '\n') {
            line++
            lineStart = index + 1
        }
        index++
    }
    return SourcePosition(line = line, column = boundedOffset - lineStart + 1)
}
