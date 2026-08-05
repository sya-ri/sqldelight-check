package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.SqlDialectSourceTerm

internal data class CreateTableBody(
    val openOffset: Int,
    val closeOffset: Int,
    val itemDepth: Int,
)

internal fun String.createTableBodies(tokens: List<SqlToken>, parenthesisDepths: IntArray): Sequence<CreateTableBody> =
    sequence {
        tokens.forEachIndexed { index, token ->
            if (!token.isTerm(SqlDialectSourceTerm.Create)) return@forEachIndexed
            val table = tokens.getOrNull(index + 1)?.takeIf { it.isTerm(SqlDialectSourceTerm.Table) } ?: return@forEachIndexed
            val statementEnd = statementEndAfter(token.startOffset)
            val open =
                sqlCharacters()
                    .dropWhile { character -> character.offset < table.endOffset }
                    .takeWhile { character -> character.offset < statementEnd }
                    .firstOrNull { character -> character.value == '(' }
                    ?: return@forEachIndexed
            val close = matchingClosingParenthesisOffset(open.offset) ?: return@forEachIndexed
            yield(
                CreateTableBody(
                    openOffset = open.offset,
                    closeOffset = close,
                    itemDepth = parenthesisDepths[open.offset] + 1,
                ),
            )
        }
    }
