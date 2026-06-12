package dev.s7a.sqldelight.check.api

internal object SqlSourceStructureParser {
    fun parse(
        source: String,
        sourcePatterns: SqlDialectSourcePatterns,
    ): SqlSourceStructure {
        val tokens = source.sqlSourceTokens()
        val normalizedTerms = tokens.map { token -> token.normalizedText }
        var statementIndex = 0
        var parenthesisDepth = 0
        var caseDepth = 0
        val contexts =
            tokens.mapIndexed { index, token ->
                val context =
                    SqlSourceTokenContext(
                        token = token,
                        index = index,
                        statementIndex = statementIndex,
                        parenthesisDepth = parenthesisDepth,
                        caseDepth = caseDepth,
                        patternMatches = sourcePatterns.matchesStartingAt(index, normalizedTerms),
                    )

                when (token.normalizedText) {
                    "(" -> parenthesisDepth++
                    ")" -> if (parenthesisDepth > 0) parenthesisDepth--
                    "case" -> caseDepth++
                    "end" -> if (caseDepth > 0) caseDepth--
                    ";" -> if (parenthesisDepth == 0 && caseDepth == 0) statementIndex++
                }
                context
            }
        return SqlSourceStructure(contexts)
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

private fun Char.isSqlStructureSymbol(): Boolean =
    when (this) {
        '(', ')', ',', ';' -> true
        else -> false
    }

private fun Char.isIdentifierStart(): Boolean =
    this == '_' || isLetter()

private fun Char.isIdentifierPart(): Boolean =
    this == '_' || isLetterOrDigit()
