package dev.s7a.sqldelight.check.core

import dev.s7a.sqldelight.check.api.SourceFile
import dev.s7a.sqldelight.check.api.SourcePosition
import dev.s7a.sqldelight.check.rule.api.SqlFacts
import dev.s7a.sqldelight.check.rule.api.SqlJoinFacts
import dev.s7a.sqldelight.check.rule.api.SqlQualifiedReferenceFacts
import dev.s7a.sqldelight.check.rule.api.SqlResultColumnFacts
import dev.s7a.sqldelight.check.rule.api.SqlSelectFacts
import dev.s7a.sqldelight.check.rule.api.SqlStatementFacts
import dev.s7a.sqldelight.check.rule.api.SqlStatementKind
import dev.s7a.sqldelight.check.rule.api.SqlTableReferenceFacts
import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

/**
 * Conservative source scanner that populates stable SQL facts.
 *
 * This scanner is intentionally limited to facts that can be derived without
 * pretending to resolve names. SQLDelight-backed adapters can replace or enrich
 * this output while preserving the public rule-api model.
 */
internal object SourceSqlFactsExtractor {
    fun extract(file: SourceFile): SqlFacts {
        val content = file.content
        val tokens = content.sqlTokens().toList()
        if (tokens.isEmpty()) return SqlFacts()

        val statements =
            content.statementRanges()
                .mapNotNull { range -> content.statementFacts(range, tokens) }
                .toList()
        return SqlFacts(statements = statements)
    }

    private fun String.statementFacts(
        range: OffsetRange,
        tokens: List<SqlToken>,
    ): SqlStatementFacts? {
        val statementTokens = tokens.filter { token -> token.startOffset in range.startOffset..<range.endOffset }
        val leading = statementTokens.leadingSqlToken(this) ?: return null
        val kind = leading.statementKind(statementTokens)
        val select = if (kind == SqlStatementKind.Select) selectFacts(leading, range, statementTokens) else null
        val tables = tableReferences(range, statementTokens)
        val joins = joinFacts(range, statementTokens, tables)
        val qualifiedReferences = qualifiedReferences(range, statementTokens)
        return SqlStatementFacts(
            kind = kind,
            range = rangeAtOffsets(range.startOffset, range.endOffset),
            select = select,
            tableReferences = tables,
            joins = joins,
            qualifiedReferences = qualifiedReferences,
        )
    }

    private fun List<SqlToken>.leadingSqlToken(content: String): SqlToken? {
        val first = firstOrNull() ?: return null
        val second = getOrNull(1)
        return if (second != null && content.nextNonWhitespaceOffset(first.endOffset)?.let { offset -> content[offset] } == ':') {
            second
        } else {
            first
        }
    }

    private fun SqlToken.statementKind(statementTokens: List<SqlToken>): SqlStatementKind =
        when (normalizedText) {
            "select" -> SqlStatementKind.Select
            "with" -> if (statementTokens.any { token -> token.isKeyword("select") }) SqlStatementKind.Select else SqlStatementKind.Other
            "insert" -> SqlStatementKind.Insert
            "update" -> SqlStatementKind.Update
            "delete" -> SqlStatementKind.Delete
            "create" -> SqlStatementKind.Create
            "alter" -> SqlStatementKind.Alter
            "drop" -> SqlStatementKind.Drop
            else -> SqlStatementKind.Other
        }

    private fun String.selectFacts(
        leading: SqlToken,
        range: OffsetRange,
        statementTokens: List<SqlToken>,
    ): SqlSelectFacts? {
        val selectToken =
            if (leading.isKeyword("select")) {
                leading
            } else {
                statementTokens.firstOrNull { token -> token.isKeyword("select") && sqlParenthesisDepthAt(token.startOffset) == 0 }
                    ?: return null
            }
        val selectDepth = sqlParenthesisDepthAt(selectToken.startOffset)
        val fromToken =
            statementTokens.firstOrNull { token ->
                token.startOffset > selectToken.endOffset &&
                    token.startOffset < range.endOffset &&
                    token.isKeyword("from") &&
                    sqlParenthesisDepthAt(token.startOffset) == selectDepth
            } ?: return null

        val columns = resultColumns(selectToken.endOffset, fromToken.startOffset, selectDepth)
        return SqlSelectFacts(
            selectListRange = rangeAtOffsets(selectToken.startOffset, fromToken.startOffset),
            resultColumns = columns,
        )
    }

    private fun String.resultColumns(
        startOffset: Int,
        endOffset: Int,
        depth: Int,
    ): List<SqlResultColumnFacts> =
        splitTopLevelSegments(startOffset, endOffset, depth)
            .map { segment ->
                val text = substring(segment.startOffset, segment.endOffset)
                val tokens = text.sqlTokens().toList()
                SqlResultColumnFacts(
                    range = rangeAtOffsets(segment.startOffset, segment.endOffset),
                    alias = tokens.aliasText(),
                    wildcard = text.trim() == "*" || text.trim().endsWith(".*"),
                )
            }

    private fun List<SqlToken>.aliasText(): String? {
        if (size < 2) return null
        val last = last()
        val previous = this[lastIndex - 1]
        if (previous.isKeyword("as")) return last.text
        return if (previous.endOffset < last.startOffset && last.normalizedText !in aliasBoundaryKeywords) last.text else null
    }

    private fun String.tableReferences(
        range: OffsetRange,
        statementTokens: List<SqlToken>,
    ): List<SqlTableReferenceFacts> {
        val references = mutableListOf<SqlTableReferenceFacts>()
        statementTokens.forEachIndexed { index, token ->
            if (!token.isKeyword("from") && !token.isKeyword("join")) return@forEachIndexed
            if (sqlParenthesisDepthAt(token.startOffset) != 0) return@forEachIndexed
            val boundary = firstReferenceBoundaryAfter(statementTokens, index + 1, range.endOffset)
            references += tableReferencesAfterKeyword(token.endOffset, boundary)
        }
        return references
    }

    private fun String.tableReferencesAfterKeyword(
        startOffset: Int,
        endOffset: Int,
    ): List<SqlTableReferenceFacts> =
        splitTopLevelSegments(startOffset, endOffset, depth = 0)
            .mapNotNull { segment -> tableReferenceInSegment(segment) }

    private fun String.tableReferenceInSegment(segment: OffsetRange): SqlTableReferenceFacts? {
        val open = nextSqlCharacterAfter(segment.startOffset)
        if (open?.value == '(' && open.offset < segment.endOffset) {
            val close = matchingClosingParenthesisOffset(open.offset) ?: return null
            val alias = substring(close + 1, segment.endOffset).sqlTokens().toList().aliasText()
            return SqlTableReferenceFacts(
                range = rangeAtOffsets(open.offset, segment.endOffset),
                alias = alias,
                subquery = true,
            )
        }

        val tokens =
            substring(segment.startOffset, segment.endOffset)
                .sqlTokens()
                .toList()
        val source = tokens.firstOrNull() ?: return null
        return SqlTableReferenceFacts(
            range = rangeAtOffsets(segment.startOffset, segment.endOffset),
            name = source.text,
            alias = tokens.drop(1).aliasText(),
            subquery = false,
        )
    }

    private fun String.joinFacts(
        range: OffsetRange,
        statementTokens: List<SqlToken>,
        references: List<SqlTableReferenceFacts>,
    ): List<SqlJoinFacts> =
        statementTokens
            .filter { token -> token.isKeyword("join") && sqlParenthesisDepthAt(token.startOffset) == 0 }
            .mapNotNull { join ->
                val table = references.firstOrNull { reference -> reference.range.start.toOffsetIn(this) >= join.endOffset } ?: return@mapNotNull null
                SqlJoinFacts(
                    range = rangeAtOffsets(join.startOffset, table.range.end.toOffsetIn(this)),
                    kind = joinKindBefore(join, range.startOffset, statementTokens),
                    table = table,
                )
            }

    private fun joinKindBefore(
        join: SqlToken,
        statementStart: Int,
        statementTokens: List<SqlToken>,
    ): String {
        val modifiers =
            statementTokens
                .filter { token -> token.startOffset in statementStart..<join.startOffset }
                .takeLastWhile { token -> token.normalizedText in joinModifierKeywords }
        return (modifiers.map { token -> token.text } + join.text).joinToString(" ")
    }

    private fun String.qualifiedReferences(
        range: OffsetRange,
        statementTokens: List<SqlToken>,
    ): List<SqlQualifiedReferenceFacts> =
        statementTokens
            .zipWithNext()
            .mapNotNull { (left, right) ->
                if (left.startOffset !in range.startOffset..<range.endOffset) return@mapNotNull null
                if (right.endOffset !in range.startOffset..range.endOffset) return@mapNotNull null
                val dot = nextSqlCharacterAfter(left.endOffset)
                if (dot?.value != '.' || dot.offset >= right.startOffset) return@mapNotNull null
                SqlQualifiedReferenceFacts(
                    range = rangeAtOffsets(left.startOffset, right.endOffset),
                    qualifier = left.text,
                    name = right.text,
                )
            }

    private fun String.statementRanges(): Sequence<OffsetRange> =
        sequence {
            var start = 0
            sqlCharacters().forEach { character ->
                if (character.value == ';') {
                    trimmedRange(start, character.offset + 1)?.let { range -> yield(range) }
                    start = character.offset + 1
                }
            }
            trimmedRange(start, length)?.let { range -> yield(range) }
        }

    private fun String.splitTopLevelSegments(
        startOffset: Int,
        endOffset: Int,
        depth: Int,
    ): List<OffsetRange> {
        val ranges = mutableListOf<OffsetRange>()
        var start = startOffset
        sqlCharacters()
            .dropWhile { character -> character.offset < startOffset }
            .takeWhile { character -> character.offset < endOffset }
            .forEach { character ->
                if (character.value == ',' && sqlParenthesisDepthAt(character.offset) == depth) {
                    trimmedRange(start, character.offset)?.let(ranges::add)
                    start = character.offset + 1
                }
            }
        trimmedRange(start, endOffset)?.let(ranges::add)
        return ranges
    }

    private fun String.trimmedRange(
        startOffset: Int,
        endOffset: Int,
    ): OffsetRange? {
        var start = startOffset
        var end = endOffset
        while (start < end && this[start].isWhitespace()) start++
        while (end > start && this[end - 1].isWhitespace()) end--
        return if (start < end) OffsetRange(start, end) else null
    }

    private fun String.firstReferenceBoundaryAfter(
        tokens: List<SqlToken>,
        startIndex: Int,
        statementEnd: Int,
    ): Int =
        tokens
            .asSequence()
            .drop(startIndex)
            .firstOrNull { token ->
                token.startOffset < statementEnd &&
                    sqlParenthesisDepthAt(token.startOffset) == 0 &&
                    token.normalizedText in tableReferenceBoundaryKeywords
            }?.startOffset
            ?: statementEnd
}

private data class OffsetRange(
    val startOffset: Int,
    val endOffset: Int,
)

private data class SqlToken(
    val text: String,
    val startOffset: Int,
    val endOffset: Int,
) {
    val normalizedText: String = text.lowercase()
}

private data class SqlCharacter(
    val value: Char,
    val offset: Int,
)

private fun SourcePosition.toOffsetIn(content: String): Int {
    var line = 1
    var column = 1
    content.forEachIndexed { index, character ->
        if (line == this.line && column == this.column) return index
        if (character == '\n') {
            line++
            column = 1
        } else {
            column++
        }
    }
    return content.length
}

private fun String.sqlTokens(): Sequence<SqlToken> =
    sequence {
        var index = 0
        while (index < length) {
            index =
                when {
                    startsWith("--", index) -> skipLineComment(index)
                    startsWith("/*", index) -> skipBlockComment(index)
                    this@sqlTokens[index] == '\'' -> skipQuoted(index, '\'')
                    this@sqlTokens[index] == '"' -> skipQuoted(index, '"')
                    this@sqlTokens[index] == '`' -> skipQuoted(index, '`')
                    this@sqlTokens[index] == '[' -> skipBracketQuoted(index)
                    this@sqlTokens[index].isIdentifierStart() -> {
                        val start = index
                        index++
                        while (index < length && this@sqlTokens[index].isIdentifierPart()) index++
                        yield(SqlToken(text = substring(start, index), startOffset = start, endOffset = index))
                        index
                    }
                    else -> index + 1
                }
        }
    }

private fun String.sqlCharacters(): Sequence<SqlCharacter> =
    sequence {
        var index = 0
        while (index < length) {
            index =
                when {
                    startsWith("--", index) -> skipLineComment(index)
                    startsWith("/*", index) -> skipBlockComment(index)
                    this@sqlCharacters[index] == '\'' -> skipQuoted(index, '\'')
                    this@sqlCharacters[index] == '"' -> skipQuoted(index, '"')
                    this@sqlCharacters[index] == '`' -> skipQuoted(index, '`')
                    this@sqlCharacters[index] == '[' -> skipBracketQuoted(index)
                    else -> {
                        yield(SqlCharacter(value = this@sqlCharacters[index], offset = index))
                        index + 1
                    }
                }
        }
    }

private fun String.sqlParenthesisDepthAt(offset: Int): Int {
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

private fun String.nextSqlCharacterAfter(offset: Int): SqlCharacter? =
    sqlCharacters()
        .dropWhile { character -> character.offset < offset }
        .firstOrNull { character -> !character.value.isWhitespace() }

private fun String.matchingClosingParenthesisOffset(openOffset: Int): Int? {
    if (getOrNull(openOffset) != '(') return null
    var depth = 0
    sqlCharacters()
        .dropWhile { character -> character.offset < openOffset }
        .forEach { character ->
            when (character.value) {
                '(' -> depth++
                ')' -> {
                    depth--
                    if (depth == 0) return character.offset
                }
            }
        }
    return null
}

private fun String.nextNonWhitespaceOffset(offset: Int): Int? {
    var index = offset
    while (index < length && this[index].isWhitespace()) index++
    return if (index < length) index else null
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

private fun SqlToken.isKeyword(value: String): Boolean = text.equals(value, ignoreCase = true)

private fun Char.isIdentifierStart(): Boolean = this == '_' || isLetter()

private fun Char.isIdentifierPart(): Boolean = this == '_' || isLetterOrDigit()

private val aliasBoundaryKeywords =
    setOf("case", "cast", "coalesce", "count", "else", "end", "false", "from", "null", "then", "true", "when")

private val tableReferenceBoundaryKeywords =
    setOf(
        "cross",
        "except",
        "full",
        "group",
        "having",
        "inner",
        "intersect",
        "join",
        "left",
        "limit",
        "offset",
        "on",
        "order",
        "right",
        "union",
        "using",
        "where",
    )

private val joinModifierKeywords = setOf("cross", "full", "inner", "join", "left", "outer", "right")
