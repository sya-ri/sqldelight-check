package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.skipSqlBracketQuoted
import dev.s7a.sqldelight.check.api.skipSqlQuoted

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
                    this@blockComments[index] == '\'' -> skipSqlQuoted(index, '\'')
                    this@blockComments[index] == '"' -> skipSqlQuoted(index, '"')
                    this@blockComments[index] == '`' -> skipSqlQuoted(index, '`')
                    this@blockComments[index] == '[' -> skipSqlBracketQuoted(index)
                    else -> index + 1
                }
        }
    }
