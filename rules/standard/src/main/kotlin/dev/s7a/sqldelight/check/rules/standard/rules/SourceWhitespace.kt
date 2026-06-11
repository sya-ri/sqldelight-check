package dev.s7a.sqldelight.check.rules.standard.rules

internal fun String.previousNonWhitespaceOffset(
    offset: Int,
    expected: Char,
): Int? {
    var index = offset - 1
    while (index >= 0 && this[index].isWhitespace()) {
        index--
    }
    return if (getOrNull(index) == expected) index else null
}

internal fun String.nextNonHorizontalWhitespace(offset: Int): Char? {
    val index = nextNonHorizontalWhitespaceOffset(offset) ?: return null
    return this[index]
}

internal fun String.nextNonHorizontalWhitespaceOffset(offset: Int): Int? {
    var index = offset
    while (index < length && (this[index] == ' ' || this[index] == '\t')) {
        index++
    }
    return if (index < length) index else null
}
