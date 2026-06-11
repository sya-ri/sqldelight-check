package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.SqlDialectSourceKeywords

internal data class TableReference(
    val statementStartOffset: Int,
    val depth: Int,
    val introducedBy: TableReferenceIntroducer,
    val sourceStartOffset: Int,
    val sourceEndOffset: Int,
    val tableName: String?,
    val alias: SqlToken?,
    val aliasUsesAs: Boolean,
    val isSubquery: Boolean,
)

internal enum class TableReferenceIntroducer {
    From,
    Join,
}

internal fun String.tableReferences(
    sourceKeywords: SqlDialectSourceKeywords = SqlDialectSourceKeywords.SourceScannerDefault,
): List<TableReference> {
    val tokens = sqlTokens().toList()
    val references = mutableListOf<TableReference>()
    tokens.forEachIndexed { index, token ->
        if (!token.isKeyword("from") && !token.isKeyword("join")) return@forEachIndexed

        val depth = sqlParenthesisDepthAt(token.startOffset)
        val statementStart = statementStartBefore(token.startOffset)
        val statementEnd = statementEndAfter(token.startOffset)
        val boundary = firstReferenceBoundaryAfter(tokens, index + 1, statementEnd, depth, sourceKeywords)
        references +=
            tableReferencesAfterKeyword(
                tokens = tokens,
                startOffset = token.endOffset,
                boundaryOffset = boundary,
                depth = depth,
                statementStart = statementStart,
                introducedBy = if (token.isKeyword("join")) TableReferenceIntroducer.Join else TableReferenceIntroducer.From,
            )
    }
    return references
}

private fun String.tableReferencesAfterKeyword(
    tokens: List<SqlToken>,
    startOffset: Int,
    boundaryOffset: Int,
    depth: Int,
    statementStart: Int,
    introducedBy: TableReferenceIntroducer,
): List<TableReference> {
    val references = mutableListOf<TableReference>()
    var segmentStart = startOffset
    sqlCharacters()
        .dropWhile { character -> character.offset < segmentStart }
        .takeWhile { character -> character.offset < boundaryOffset }
        .forEach { character ->
            if (character.value == ',' && sqlParenthesisDepthAt(character.offset) == depth) {
                tableReferenceInSegment(tokens, segmentStart, character.offset, depth, statementStart, introducedBy)?.let(references::add)
                segmentStart = character.offset + 1
            }
        }
    tableReferenceInSegment(tokens, segmentStart, boundaryOffset, depth, statementStart, introducedBy)?.let(references::add)
    return references
}

private fun String.tableReferenceInSegment(
    tokens: List<SqlToken>,
    startOffset: Int,
    endOffset: Int,
    depth: Int,
    statementStart: Int,
    introducedBy: TableReferenceIntroducer,
): TableReference? {
    val open = nextSqlCharacterAfter(startOffset)
    if (open?.value == '(' && open.offset < endOffset) {
        val closeOffset = matchingClosingParenthesisOffset(open.offset) ?: return null
        if (closeOffset >= endOffset) return null
        val select =
            tokens.firstOrNull { token ->
                token.startOffset > open.offset &&
                    token.startOffset < closeOffset &&
                    token.isKeyword("select") &&
                    sqlParenthesisDepthAt(token.startOffset) == depth + 1
            } ?: return null
        if (tokens.any { token -> token.startOffset in (open.offset + 1)..<select.startOffset }) return null

        val alias = aliasAfterSource(tokens, closeOffset + 1, endOffset, depth)
        return TableReference(
            statementStartOffset = statementStart,
            depth = depth,
            introducedBy = introducedBy,
            sourceStartOffset = open.offset,
            sourceEndOffset = closeOffset + 1,
            tableName = null,
            alias = alias?.token,
            aliasUsesAs = alias?.usesAs == true,
            isSubquery = true,
        )
    }

    val segmentTokens =
        tokens.filter { token ->
            token.startOffset >= startOffset &&
                token.endOffset <= endOffset &&
                sqlParenthesisDepthAt(token.startOffset) == depth
        }
    val firstToken = segmentTokens.firstOrNull() ?: return null

    val sourceTokens = qualifiedIdentifierTokens(segmentTokens)
    val tableName = sourceTokens.last().text
    val alias = aliasAfterSource(tokens, sourceTokens.last().endOffset, endOffset, depth)
    return TableReference(
        statementStartOffset = statementStart,
        depth = depth,
        introducedBy = introducedBy,
        sourceStartOffset = firstToken.startOffset,
        sourceEndOffset = firstToken.endOffset,
        tableName = tableName,
        alias = alias?.token,
        aliasUsesAs = alias?.usesAs == true,
        isSubquery = false,
    )
}

private fun String.qualifiedIdentifierTokens(segmentTokens: List<SqlToken>): List<SqlToken> {
    val sourceTokens = mutableListOf(segmentTokens.first())
    var previous = segmentTokens.first()
    segmentTokens.drop(1).forEach { token ->
        val dot = nextSqlCharacterAfter(previous.endOffset)
        if (dot?.value != '.' || dot.offset >= token.startOffset) return sourceTokens
        sourceTokens += token
        previous = token
    }
    return sourceTokens
}

private data class AliasToken(
    val token: SqlToken,
    val usesAs: Boolean,
)

private fun String.aliasAfterSource(
    tokens: List<SqlToken>,
    startOffset: Int,
    endOffset: Int,
    depth: Int,
): AliasToken? {
    val aliasTokens =
        tokens.filter { token ->
            token.startOffset >= startOffset &&
                token.endOffset <= endOffset &&
                sqlParenthesisDepthAt(token.startOffset) == depth
        }
    if (aliasTokens.isEmpty()) return null
    val first = aliasTokens.first()
    return if (first.isKeyword("as")) {
        aliasTokens.getOrNull(1)?.let { token -> AliasToken(token = token, usesAs = true) }
    } else {
        AliasToken(token = first, usesAs = false)
    }
}

private fun String.firstReferenceBoundaryAfter(
    tokens: List<SqlToken>,
    startIndex: Int,
    statementEnd: Int,
    depth: Int,
    sourceKeywords: SqlDialectSourceKeywords,
): Int =
    tokens
        .asSequence()
        .drop(startIndex)
        .firstOrNull { token ->
                token.startOffset < statementEnd &&
                sqlParenthesisDepthAt(token.startOffset) == depth &&
                token.normalizedText in sourceKeywords.tableReferenceBoundaryKeywords
        }?.startOffset
        ?: statementEnd

private fun String.statementStartBefore(offset: Int): Int =
    sqlCharacters()
        .takeWhile { character -> character.offset < offset }
        .lastOrNull { character -> character.value == ';' }
        ?.let { character -> character.offset + 1 }
        ?: 0
