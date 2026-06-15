package dev.s7a.sqldelight.check.api

/**
 * Returns the offset immediately after a SQL quoted range that starts at [start].
 *
 * The scanner treats doubled quote characters as escaped quotes, except for
 * backtick-quoted identifiers where the first closing backtick ends the range.
 */
public fun String.skipSqlQuoted(
    start: Int,
    quote: Char,
): Int {
    var index = start + 1
    while (index < length) {
        if (this[index] == quote) {
            if (quote != '`' && index + 1 < length && this[index + 1] == quote) {
                index += 2
            } else {
                return index + 1
            }
        } else {
            index++
        }
    }
    return length
}

/**
 * Returns the offset immediately after a bracket-quoted SQL identifier.
 *
 * Doubled closing brackets are treated as escaped bracket characters inside
 * the quoted identifier.
 */
public fun String.skipSqlBracketQuoted(start: Int): Int {
    var index = start + 1
    while (index < length) {
        if (this[index] == ']') {
            if (index + 1 < length && this[index + 1] == ']') {
                index += 2
            } else {
                return index + 1
            }
        } else {
            index++
        }
    }
    return length
}
