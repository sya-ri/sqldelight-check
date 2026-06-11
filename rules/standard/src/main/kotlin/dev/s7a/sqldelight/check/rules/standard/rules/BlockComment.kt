package dev.s7a.sqldelight.check.rules.standard.rules

internal data class BlockComment(
    val startOffset: Int,
    val endOffset: Int,
)

internal fun String.blockComments(): Sequence<BlockComment> =
    sequence {
        var index = 0
        while (index < length) {
            index =
                when {
                    startsWith("--", index) -> skipLineComment(index)
                    startsWith("/*", index) -> {
                        val end = skipBlockComment(index)
                        yield(BlockComment(startOffset = index, endOffset = end))
                        end
                    }
                    this@blockComments[index] == '\'' -> skipQuoted(index, '\'')
                    this@blockComments[index] == '"' -> skipQuoted(index, '"')
                    this@blockComments[index] == '`' -> skipQuoted(index, '`')
                    this@blockComments[index] == '[' -> skipBracketQuoted(index)
                    else -> index + 1
                }
        }
    }
