package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.skipSqlBracketQuoted
import dev.s7a.sqldelight.check.api.skipSqlQuoted

internal data class SqlCharacter(
    val value: Char,
    val offset: Int,
)

internal fun String.sqlCharacters(): Sequence<SqlCharacter> =
    sequence {
        var index = 0
        while (index < length) {
            index =
                when {
                    startsWith("--", index) -> skipLineComment(index)
                    startsWith("/*", index) -> skipBlockComment(index)
                    this@sqlCharacters[index] == '\'' -> skipSqlQuoted(index, '\'')
                    this@sqlCharacters[index] == '"' -> skipSqlQuoted(index, '"')
                    this@sqlCharacters[index] == '`' -> skipSqlQuoted(index, '`')
                    this@sqlCharacters[index] == '[' -> skipSqlBracketQuoted(index)
                    else -> {
                        yield(SqlCharacter(value = this@sqlCharacters[index], offset = index))
                        index + 1
                    }
                }
        }
    }

internal fun String.statementEndAfter(offset: Int): Int =
    sqlCharacters()
        .firstOrNull { character -> character.offset >= offset && character.value == ';' }
        ?.offset
        ?: length

internal fun String.topLevelSemicolonOffsets(): List<Int> {
    var depth = 0
    val offsets = mutableListOf<Int>()
    sqlCharacters().forEach { character ->
        when (character.value) {
            '(' -> depth++
            ')' -> depth = (depth - 1).coerceAtLeast(0)
            ';' -> if (depth == 0) offsets += character.offset
        }
    }
    return offsets
}

internal fun String.sqlParenthesisDepthAt(offset: Int): Int {
    var depth = 0
    sqlCharacters()
        .takeWhile { character -> character.offset < offset }
        .forEach { character ->
            when (character.value) {
                '(' -> depth++
                ')' -> if (depth > 0) depth--
            }
        }
    return depth
}

internal fun String.previousSqlCharacterBefore(offset: Int): SqlCharacter? =
    sqlCharacters()
        .takeWhile { character -> character.offset < offset }
        .filterNot { character -> character.value.isWhitespace() }
        .lastOrNull()

internal fun String.nextSqlCharacterAfter(offset: Int): SqlCharacter? =
    sqlCharacters()
        .dropWhile { character -> character.offset < offset }
        .firstOrNull { character -> !character.value.isWhitespace() }

internal fun String.matchingClosingParenthesisOffset(openOffset: Int): Int? {
    return matchingClosingDelimiterOffset(openOffset, open = '(', close = ')')
}

internal fun String.matchingClosingBraceOffset(openOffset: Int): Int? {
    return matchingClosingDelimiterOffset(openOffset, open = '{', close = '}')
}

private fun String.matchingClosingDelimiterOffset(
    openOffset: Int,
    open: Char,
    close: Char,
): Int? {
    if (getOrNull(openOffset) != open) return null

    var depth = 0
    sqlCharacters()
        .dropWhile { character -> character.offset < openOffset }
        .forEach { character ->
            when (character.value) {
                open -> depth++
                close -> {
                    depth--
                    if (depth == 0) return character.offset
                }
            }
        }
    return null
}
