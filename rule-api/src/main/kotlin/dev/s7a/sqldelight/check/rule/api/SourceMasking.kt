package dev.s7a.sqldelight.check.rule.api

/**
 * Replaces comments and quoted text with spaces while preserving offsets.
 */
public fun String.maskSqlCommentsAndQuotedText(hashLineComments: Boolean = false): String {
    val chars = toCharArray()
    var index = 0
    while (index < chars.size) {
        index =
            when {
                startsWith("--", index) -> maskLineComment(chars, index)
                hashLineComments && chars[index] == '#' -> maskLineComment(chars, index)
                startsWith("/*", index) -> maskBlockComment(chars, index)
                chars[index] == '\'' -> maskQuoted(chars, index, '\'')
                chars[index] == '"' -> maskQuoted(chars, index, '"')
                chars[index] == '`' -> maskQuoted(chars, index, '`')
                chars[index] == '[' -> maskBracketQuoted(chars, index)
                else -> index + 1
            }
    }
    return String(chars)
}

private fun String.maskLineComment(
    chars: CharArray,
    start: Int,
): Int {
    val end = indexOf('\n', startIndex = start).let { if (it == -1) length else it }
    for (index in start until end) chars[index] = ' '
    return end
}

private fun String.maskBlockComment(
    chars: CharArray,
    start: Int,
): Int {
    val end = indexOf("*/", startIndex = start + 2).let { if (it == -1) length else it + 2 }
    for (index in start until end) chars[index] = ' '
    return end
}

private fun maskQuoted(
    chars: CharArray,
    start: Int,
    quote: Char,
): Int {
    var index = start + 1
    chars[start] = ' '
    while (index < chars.size) {
        val current = chars[index]
        chars[index] = ' '
        if (current == quote) {
            val next = index + 1
            if (next < chars.size && chars[next] == quote) {
                chars[next] = ' '
                index += 2
            } else {
                return next
            }
        } else {
            index++
        }
    }
    return chars.size
}

private fun String.maskBracketQuoted(
    chars: CharArray,
    start: Int,
): Int {
    chars[start] = ' '
    val end = indexOf(']', startIndex = start + 1).let { if (it == -1) length else it + 1 }
    for (index in start + 1 until end) chars[index] = ' '
    return end
}
