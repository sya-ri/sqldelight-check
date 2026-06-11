package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.SourcePosition

internal fun SourcePosition.toOffsetIn(content: String): Int {
    var line = 1
    var column = 1
    content.forEachIndexed { index, character ->
        if (line == this.line && column == this.column) return index
        if (character == '\n') {
            line++
            column = 1
        } else {
            column++
        }
    }
    return content.length
}
