package dev.s7a.sqldelight.check.rules.standard.rules

internal data class LineComment(
    val startOffset: Int,
    val endOffset: Int,
)

internal fun String.lineComments(): Sequence<LineComment> =
    sequence {
        var index = 0
        while (index < length) {
            index =
                when {
                    startsWith("--", index) -> {
                        val end = skipLineComment(index)
                        yield(LineComment(startOffset = index, endOffset = end))
                        end
                    }
                    startsWith("/*", index) -> skipBlockComment(index)
                    this@lineComments[index] == '\'' -> skipQuoted(index, '\'')
                    this@lineComments[index] == '"' -> skipQuoted(index, '"')
                    this@lineComments[index] == '`' -> skipQuoted(index, '`')
                    this@lineComments[index] == '[' -> skipBracketQuoted(index)
                    else -> index + 1
                }
        }
    }
