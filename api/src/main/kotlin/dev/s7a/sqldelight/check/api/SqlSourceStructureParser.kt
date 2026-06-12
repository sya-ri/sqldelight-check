package dev.s7a.sqldelight.check.api

internal object SqlSourceStructureParser {
    fun parse(
        source: String,
        sourcePatterns: SqlDialectSourcePatterns,
    ): SqlSourceStructure {
        val tokens = source.sqlSourceTokens()
        val normalizedTerms = tokens.map { token -> token.normalizedText }
        val blockPatterns = sourcePatterns.blockPatterns
        var statementIndex = 0
        var parenthesisDepth = 0
        var caseDepth = 0
        val contexts =
            tokens.mapIndexed { index, token ->
                val remainingTerms = normalizedTerms.subList(index, normalizedTerms.size)
                val context =
                    SqlSourceTokenContext(
                        token = token,
                        index = index,
                        statementIndex = statementIndex,
                        parenthesisDepth = parenthesisDepth,
                        caseDepth = caseDepth,
                        patternMatches = sourcePatterns.matchesStartingAt(index, normalizedTerms),
                    )

                if (blockPatterns.isParenthesisOpen(token)) {
                    parenthesisDepth++
                } else if (blockPatterns.isParenthesisClose(token) && parenthesisDepth > 0) {
                    parenthesisDepth--
                }
                if (blockPatterns.startsCaseExpression(remainingTerms)) {
                    caseDepth++
                } else if (blockPatterns.endsCaseExpression(remainingTerms) && caseDepth > 0) {
                    caseDepth--
                }
                if (token.normalizedText in blockPatterns.statementSeparatorTerms &&
                    parenthesisDepth == 0 &&
                    caseDepth == 0
                ) {
                    statementIndex++
                }
                context
            }
        return SqlSourceStructure(
            tokens = contexts,
            blocks = contexts.sourceBlocks(blockPatterns),
        )
    }

    private fun SqlDialectSourcePatterns.matchesStartingAt(
        index: Int,
        normalizedTerms: List<String>,
    ): Set<SqlSourcePatternMatch> {
        val terms = normalizedTerms.subList(index, normalizedTerms.size)
        return patterns
            .mapNotNullTo(mutableSetOf()) { pattern ->
                val length = pattern.expression.matchPrefix(terms) ?: return@mapNotNullTo null
                SqlSourcePatternMatch(pattern = pattern, length = length)
            }
    }
}

private fun List<SqlSourceTokenContext>.sourceBlocks(blockPatterns: SqlDialectSourceBlockPatterns): List<SqlSourceBlock> {
    if (isEmpty()) return emptyList()
    val pending = mutableListOf<PendingSqlSourceBlock>()
    addStatementBlocksTo(pending)
    addParenthesizedBlocksTo(pending, blockPatterns.parenthesizedBlocks)
    addPairedBlocksTo(pending, blockPatterns.pairedBlocks)
    addClauseBlocksTo(pending, blockPatterns.clauseStartRoles)
    return pending.toSourceBlocks(this)
}

private fun SqlDialectSourceBlockPatterns.isParenthesisOpen(token: SqlSourceToken): Boolean {
    parenthesisDepthTerms.forEach { terms ->
        if (token.normalizedText == terms.normalizedOpenTerm) return true
    }
    return false
}

private fun SqlDialectSourceBlockPatterns.isParenthesisClose(token: SqlSourceToken): Boolean {
    parenthesisDepthTerms.forEach { terms ->
        if (token.normalizedText == terms.normalizedCloseTerm) return true
    }
    return false
}

private fun SqlDialectSourceBlockPatterns.startsCaseExpression(terms: List<String>): Boolean {
    pairedBlocks.forEach { pattern ->
        if (pattern.kind == SqlSourceBlockKind.CaseExpression && pattern.startExpression.matchPrefix(terms) != null) {
            return true
        }
    }
    return false
}

private fun SqlDialectSourceBlockPatterns.endsCaseExpression(terms: List<String>): Boolean {
    pairedBlocks.forEach { pattern ->
        if (pattern.kind == SqlSourceBlockKind.CaseExpression && pattern.endExpression.matchPrefix(terms) != null) {
            return true
        }
    }
    return false
}

private fun List<SqlSourceTokenContext>.addStatementBlocksTo(blocks: MutableList<PendingSqlSourceBlock>) {
    var startIndex = 0
    while (startIndex < size) {
        val statementIndex = this[startIndex].statementIndex
        var endIndex = startIndex + 1
        while (endIndex < size && this[endIndex].statementIndex == statementIndex) {
            endIndex++
        }
        blocks +=
            PendingSqlSourceBlock(
                kind = SqlSourceBlockKind.Statement,
                startTokenIndex = startIndex,
                endTokenIndex = endIndex,
            )
        startIndex = endIndex
    }
}

private class ParenthesizedBlockStart(
    val pattern: SqlDialectSourceParenthesizedBlockPattern,
    val tokenIndex: Int,
)

private fun List<SqlSourceTokenContext>.addParenthesizedBlocksTo(
    blocks: MutableList<PendingSqlSourceBlock>,
    patterns: Set<SqlDialectSourceParenthesizedBlockPattern>,
) {
    val starts = mutableListOf<ParenthesizedBlockStart>()
    forEach { context ->
        patterns.forEach { pattern ->
            if (context.token.normalizedText == pattern.normalizedOpenTerm) {
                starts += ParenthesizedBlockStart(pattern = pattern, tokenIndex = context.index)
            } else if (context.token.normalizedText == pattern.normalizedCloseTerm) {
                val start = starts.removeLastMatching(pattern) ?: return@forEach
                blocks +=
                    PendingSqlSourceBlock(
                        kind = parenthesizedBlockKind(start, context.index),
                        startTokenIndex = start.tokenIndex,
                        endTokenIndex = context.index + 1,
                    )
            }
        }
    }
}

private fun MutableList<ParenthesizedBlockStart>.removeLastMatching(
    pattern: SqlDialectSourceParenthesizedBlockPattern,
): ParenthesizedBlockStart? {
    for (index in lastIndex downTo 0) {
        if (this[index].pattern == pattern) {
            return removeAt(index)
        }
    }
    return null
}

private fun List<SqlSourceTokenContext>.parenthesizedBlockKind(
    start: ParenthesizedBlockStart,
    endTokenIndex: Int,
): SqlSourceBlockKind {
    val inner = getOrNull(start.tokenIndex + 1)
    return if (inner != null && inner.index < endTokenIndex && inner.matchesAny(start.pattern.innerStartRoles)) {
        start.pattern.innerStartKind
    } else {
        start.pattern.defaultKind
    }
}

private fun SqlSourceTokenContext.matchesAny(roles: Set<SqlDialectSourcePatternRole>): Boolean {
    roles.forEach { role ->
        if (matches(role)) return true
    }
    return false
}

private class PairedBlockStart(
    val pattern: SqlDialectSourcePairedBlockPattern,
    val tokenIndex: Int,
)

private fun List<SqlSourceTokenContext>.addPairedBlocksTo(
    blocks: MutableList<PendingSqlSourceBlock>,
    patterns: Set<SqlDialectSourcePairedBlockPattern>,
) {
    val starts = mutableListOf<PairedBlockStart>()
    val normalizedTerms = map { context -> context.token.normalizedText }
    forEachIndexed { index, context ->
        val remainingTerms = normalizedTerms.subList(index, normalizedTerms.size)
        patterns.forEach { pattern ->
            if (pattern.endExpression.matchPrefix(remainingTerms) != null) {
                val start = starts.removeLastMatching(pattern) ?: return@forEach
                blocks +=
                    PendingSqlSourceBlock(
                        kind = pattern.kind,
                        startTokenIndex = start.tokenIndex,
                        endTokenIndex = context.index + 1,
                    )
            } else if (pattern.startExpression.matchPrefix(remainingTerms) != null) {
                starts += PairedBlockStart(pattern = pattern, tokenIndex = context.index)
            }
        }
    }
}

private fun MutableList<PairedBlockStart>.removeLastMatching(
    pattern: SqlDialectSourcePairedBlockPattern,
): PairedBlockStart? {
    for (index in lastIndex downTo 0) {
        if (this[index].pattern == pattern) {
            return removeAt(index)
        }
    }
    return null
}

private fun List<SqlSourceTokenContext>.addClauseBlocksTo(
    blocks: MutableList<PendingSqlSourceBlock>,
    clauseStartRoles: Set<SqlDialectSourcePatternRole>,
) {
    val starts = clauseStarts(clauseStartRoles)
    starts.forEachIndexed { index, start ->
        blocks +=
            PendingSqlSourceBlock(
                kind = SqlSourceBlockKind.Clause,
                startTokenIndex = start.context.index,
                endTokenIndex = clauseEndTokenIndex(start, starts, index + 1),
                sourcePatternMatch = start.match,
            )
    }
}

private fun List<SqlSourceTokenContext>.clauseStarts(
    clauseStartRoles: Set<SqlDialectSourcePatternRole>,
): List<SqlSourceClauseStart> =
    buildList {
        var index = 0
        while (index < this@clauseStarts.size) {
            val context = this@clauseStarts[index]
            val match = context.bestClauseStartMatch(clauseStartRoles)
            if (match == null) {
                index++
            } else {
                add(SqlSourceClauseStart(context = context, match = match))
                index += match.length.coerceAtLeast(1)
            }
        }
    }

private fun SqlSourceTokenContext.bestClauseStartMatch(
    clauseStartRoles: Set<SqlDialectSourcePatternRole>,
): SqlSourcePatternMatch? {
    var result: SqlSourcePatternMatch? = null
    patternMatches.forEach { match ->
        val current = result
        if (match.hasAnyRole(clauseStartRoles) && (current == null || match.length > current.length)) {
            result = match
        }
    }
    return result
}

private fun SqlSourcePatternMatch.hasAnyRole(targetRoles: Set<SqlDialectSourcePatternRole>): Boolean {
    targetRoles.forEach { role ->
        if (role in roles) return true
    }
    return false
}

private fun List<SqlSourceTokenContext>.clauseEndTokenIndex(
    start: SqlSourceClauseStart,
    starts: List<SqlSourceClauseStart>,
    nextStartIndex: Int,
): Int {
    for (index in nextStartIndex..<starts.size) {
        val candidate = starts[index]
        if (candidate.context.isSameBlockLevelAs(start.context)) {
            return candidate.context.index
        }
    }
    return statementEndTokenIndex(start.context.statementIndex)
}

private fun SqlSourceTokenContext.isSameBlockLevelAs(other: SqlSourceTokenContext): Boolean =
    statementIndex == other.statementIndex &&
        parenthesisDepth == other.parenthesisDepth &&
        caseDepth == other.caseDepth

private fun List<SqlSourceTokenContext>.statementEndTokenIndex(statementIndex: Int): Int {
    forEach { context ->
        if (context.statementIndex > statementIndex) {
            return context.index
        }
    }
    return size
}

private fun List<PendingSqlSourceBlock>.toSourceBlocks(tokens: List<SqlSourceTokenContext>): List<SqlSourceBlock> =
    buildList {
        this@toSourceBlocks.forEachIndexed { index, block ->
            val start = tokens[block.startTokenIndex]
            val end = tokens[block.endTokenIndex - 1]
            add(
                SqlSourceBlock(
                    kind = block.kind,
                    startTokenIndex = block.startTokenIndex,
                    endTokenIndex = block.endTokenIndex,
                    startOffset = start.token.startOffset,
                    endOffset = end.token.endOffset,
                    statementIndex = start.statementIndex,
                    parentBlockIndex = parentBlockIndex(index),
                    sourcePatternMatch = block.sourcePatternMatch,
                ),
            )
        }
    }

private fun List<PendingSqlSourceBlock>.parentBlockIndex(blockIndex: Int): Int? {
    val block = this[blockIndex]
    var result: Int? = null
    forEachIndexed { index, candidate ->
        if (index != blockIndex && candidate.canContain(block)) {
            val current = result
            if (current == null || candidate.size < this[current].size) {
                result = index
            }
        }
    }
    return result
}

private fun String.sqlSourceTokens(): List<SqlSourceToken> =
    buildList {
        var index = 0
        while (index < length) {
            index =
                when {
                    startsWith("--", index) -> skipLineComment(index)
                    startsWith("/*", index) -> skipBlockComment(index)
                    this@sqlSourceTokens[index] == '\'' -> skipQuoted(index, '\'')
                    this@sqlSourceTokens[index] == '"' -> skipQuoted(index, '"')
                    this@sqlSourceTokens[index] == '`' -> skipQuoted(index, '`')
                    this@sqlSourceTokens[index] == '[' -> skipBracketQuoted(index)
                    this@sqlSourceTokens[index] == '$' -> skipDollarQuoted(index) ?: readSymbol(this@sqlSourceTokens, index)
                    this@sqlSourceTokens.isNamedParameterStart(index) -> skipNamedParameter(index)
                    this@sqlSourceTokens[index].isIdentifierStart() -> readIdentifier(this@sqlSourceTokens, index)
                    this@sqlSourceTokens[index].isDigit() -> readNumber(this@sqlSourceTokens, index)
                    this@sqlSourceTokens[index].isSqlStructureSymbol() -> readSymbol(this@sqlSourceTokens, index)
                    this@sqlSourceTokens[index].isWhitespace() -> index + 1
                    else -> readSymbol(this@sqlSourceTokens, index)
                }
        }
    }

private fun MutableList<SqlSourceToken>.readIdentifier(
    source: String,
    start: Int,
): Int {
    var index = start + 1
    while (index < source.length && source[index].isIdentifierPart()) {
        index++
    }
    add(SqlSourceToken(text = source.substring(start, index), startOffset = start, endOffset = index))
    return index
}

private fun MutableList<SqlSourceToken>.readNumber(
    source: String,
    start: Int,
): Int {
    var index = start + 1
    while (index < source.length && (source[index].isDigit() || source[index] == '.')) {
        index++
    }
    add(SqlSourceToken(text = source.substring(start, index), startOffset = start, endOffset = index))
    return index
}

private fun MutableList<SqlSourceToken>.readSymbol(
    source: String,
    start: Int,
): Int {
    add(SqlSourceToken(text = source[start].toString(), startOffset = start, endOffset = start + 1))
    return start + 1
}

private fun String.skipLineComment(start: Int): Int {
    val newline = indexOf('\n', startIndex = start + 2)
    return if (newline == -1) length else newline + 1
}

private fun String.skipBlockComment(start: Int): Int {
    val end = indexOf("*/", startIndex = start + 2)
    return if (end == -1) length else end + 2
}

private fun String.skipQuoted(
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

private fun String.skipBracketQuoted(start: Int): Int {
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

private fun String.skipDollarQuoted(start: Int): Int? {
    val endOfTag = indexOf('$', startIndex = start + 1)
    if (endOfTag == -1) return null
    val tag = substring(start + 1, endOfTag)
    if (tag.isNotEmpty() && !tag.first().isIdentifierStart()) return null
    if (tag.any { character -> !character.isIdentifierPart() }) return null

    val delimiter = substring(start, endOfTag + 1)
    val end = indexOf(delimiter, startIndex = endOfTag + 1)
    return if (end == -1) length else end + delimiter.length
}

private fun String.isNamedParameterStart(index: Int): Boolean =
    this[index] == ':' &&
        getOrNull(index + 1)?.isIdentifierStart() == true &&
        getOrNull(index - 1) != ':'

private fun String.skipNamedParameter(start: Int): Int {
    var index = start + 2
    while (index < length && this[index].isIdentifierPart()) {
        index++
    }
    return index
}

private fun Char.isSqlStructureSymbol(): Boolean =
    when (this) {
        '(', ')', ',', ';' -> true
        else -> false
    }

private fun Char.isIdentifierStart(): Boolean =
    this == '_' || isLetter()

private fun Char.isIdentifierPart(): Boolean =
    this == '_' || isLetterOrDigit()
