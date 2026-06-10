package dev.s7a.sqldelight.check.adapter.v232

import dev.s7a.sqldelight.check.api.SourcePosition
import dev.s7a.sqldelight.check.api.SourceRange

/**
 * Parsed SQLDelight compiler error text.
 */
internal data class SqlDelightErrorMessage(
    val path: String,
    val message: String,
    val range: SourceRange,
) {
    companion object {
        private val pattern = Regex("""^(.+):(\d+):(\d+)\s+([\s\S]*)$""")

        fun parse(value: String): SqlDelightErrorMessage? {
            val match = pattern.matchEntire(value.trimEnd()) ?: return null
            val line = match.groupValues[2].toIntOrNull() ?: return null
            val zeroBasedColumn = match.groupValues[3].toIntOrNull() ?: return null
            val message = match.groupValues[4]
            val column = zeroBasedColumn + 1
            val caretWidth = message.caretWidth()
            return SqlDelightErrorMessage(
                path = match.groupValues[1],
                message = message,
                range =
                    SourceRange(
                        start = SourcePosition(line = line, column = column),
                        end = SourcePosition(line = line, column = column + caretWidth.coerceAtLeast(1)),
                    ),
            )
        }
    }
}

private fun String.caretWidth(): Int =
    lineSequence()
        .mapNotNull { line -> caretRun.find(line)?.value?.length }
        .firstOrNull()
        ?: 1

private val caretRun = Regex("""\^+""")
