package dev.s7a.sqldelight.check.core

import dev.s7a.sqldelight.check.api.SourceFile
import dev.s7a.sqldelight.check.api.SourcePosition
import dev.s7a.sqldelight.check.api.SourceRange
import dev.s7a.sqldelight.check.api.SqlDialect
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatterns
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.AliasBoundary
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.JoinModifier
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.TableReferenceBoundary
import dev.s7a.sqldelight.check.api.skipSqlBracketQuoted
import dev.s7a.sqldelight.check.api.skipSqlQuoted
import dev.s7a.sqldelight.check.rule.api.SqlFacts
import dev.s7a.sqldelight.check.rule.api.SqlJoinFacts
import dev.s7a.sqldelight.check.rule.api.SqlQualifiedReferenceFacts
import dev.s7a.sqldelight.check.rule.api.SqlResultColumnFacts
import dev.s7a.sqldelight.check.rule.api.SqlSelectFacts
import dev.s7a.sqldelight.check.rule.api.SqlStatementFacts
import dev.s7a.sqldelight.check.rule.api.SqlStatementKind
import dev.s7a.sqldelight.check.rule.api.SqlTableReferenceFacts

/**
 * Conservative source scanner that populates stable SQL facts.
 *
 * This scanner is intentionally limited to facts that can be derived without
 * pretending to resolve names. SQLDelight-backed adapters can replace or enrich
 * this output while preserving the public rule-api model.
 */
internal object SourceSqlFactsExtractor {
    fun extract(
        file: SourceFile,
        dialect: SqlDialect,
        phaseTrace: ((AnalysisPhase, Long) -> Unit)? = null,
    ): SqlFacts {
        val content = file.content
        val scanner = SqlSourceScanner(content)
        val tokens = measurePhase(phaseTrace, AnalysisPhase.Tokenization) { scanner.sqlTokens() }
        if (tokens.isEmpty()) return SqlFacts()

        val statements =
            measurePhase(phaseTrace, AnalysisPhase.FactExtraction) {
                scanner
                    .statementRanges()
                    .mapNotNull { range -> scanner.statementFacts(range, dialect.sourcePatterns) }
                    .toList()
            }
        return SqlFacts(statements = statements)
    }

    private fun SqlSourceScanner.statementFacts(
        range: OffsetRange,
        sourcePatterns: SqlDialectSourcePatterns,
    ): SqlStatementFacts? {
        val statementTokens = tokensIn(range)
        val leading = statementTokens.leadingSqlToken(content) ?: return null
        val kind = leading.statementKind(statementTokens)
        val select = if (kind == SqlStatementKind.Select) selectFacts(leading, range, statementTokens, sourcePatterns) else null
        val tables = tableReferences(range, statementTokens, sourcePatterns)
        val joins = joinFacts(range, statementTokens, tables, sourcePatterns)
        val qualifiedReferences = qualifiedReferences(range, statementTokens)
        return SqlStatementFacts(
            kind = kind,
            range = sourceRange(range.startOffset, range.endOffset),
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

    private fun SqlSourceScanner.selectFacts(
        leading: SqlToken,
        range: OffsetRange,
        statementTokens: List<SqlToken>,
        sourcePatterns: SqlDialectSourcePatterns,
    ): SqlSelectFacts? {
        val selectToken =
            if (leading.isKeyword("select")) {
                leading
            } else {
                statementTokens.firstOrNull { token -> token.isKeyword("select") && parenthesisDepthAt(token.startOffset) == 0 }
                    ?: return null
            }
        val selectDepth = parenthesisDepthAt(selectToken.startOffset)
        val fromToken =
            statementTokens.firstOrNull { token ->
                token.startOffset > selectToken.endOffset &&
                    token.startOffset < range.endOffset &&
                    token.isKeyword("from") &&
                    parenthesisDepthAt(token.startOffset) == selectDepth
            } ?: return null

        val columns = resultColumns(selectToken.endOffset, fromToken.startOffset, selectDepth, sourcePatterns)
        return SqlSelectFacts(
            selectListRange = sourceRange(selectToken.startOffset, fromToken.startOffset),
            resultColumns = columns,
        )
    }

    private fun SqlSourceScanner.resultColumns(
        startOffset: Int,
        endOffset: Int,
        depth: Int,
        sourcePatterns: SqlDialectSourcePatterns,
    ): List<SqlResultColumnFacts> =
        splitTopLevelSegments(startOffset, endOffset, depth)
            .map { segment ->
                val text = content.substring(segment.startOffset, segment.endOffset)
                val tokens = tokensIn(segment)
                SqlResultColumnFacts(
                    range = sourceRange(segment.startOffset, segment.endOffset),
                    alias = tokens.aliasText(sourcePatterns),
                    wildcard = text.trim() == "*" || text.trim().endsWith(".*"),
                )
            }

    private fun List<SqlToken>.aliasText(sourcePatterns: SqlDialectSourcePatterns): String? {
        if (size < 2) return null
        val last = last()
        val previous = this[lastIndex - 1]
        if (previous.isKeyword("as")) return last.text
        return if (previous.endOffset < last.startOffset && !sourcePatterns.matches(AliasBoundary, last)) last.text else null
    }

    private fun SqlSourceScanner.tableReferences(
        range: OffsetRange,
        statementTokens: List<SqlToken>,
        sourcePatterns: SqlDialectSourcePatterns,
    ): List<SqlTableReferenceFacts> {
        val references = mutableListOf<SqlTableReferenceFacts>()
        statementTokens.forEachIndexed { index, token ->
            if (!token.isKeyword("from") && !token.isKeyword("join")) return@forEachIndexed
            if (parenthesisDepthAt(token.startOffset) != 0) return@forEachIndexed
            val boundary = firstReferenceBoundaryAfter(statementTokens, index + 1, range.endOffset, sourcePatterns)
            references += tableReferencesAfterKeyword(token.endOffset, boundary, sourcePatterns)
        }
        return references
    }

    private fun SqlSourceScanner.tableReferencesAfterKeyword(
        startOffset: Int,
        endOffset: Int,
        sourcePatterns: SqlDialectSourcePatterns,
    ): List<SqlTableReferenceFacts> =
        splitTopLevelSegments(startOffset, endOffset, depth = 0)
            .mapNotNull { segment -> tableReferenceInSegment(segment, sourcePatterns) }

    private fun SqlSourceScanner.tableReferenceInSegment(
        segment: OffsetRange,
        sourcePatterns: SqlDialectSourcePatterns,
    ): SqlTableReferenceFacts? {
        val open = nextSqlCharacterAfter(segment.startOffset)
        if (open?.value == '(' && open.offset < segment.endOffset) {
            val close = matchingClosingParenthesisOffset(open.offset) ?: return null
            val alias = tokensIn(OffsetRange(close + 1, segment.endOffset)).aliasText(sourcePatterns)
            return SqlTableReferenceFacts(
                range = sourceRange(open.offset, segment.endOffset),
                alias = alias,
                subquery = true,
            )
        }

        val tokens = tokensIn(segment)
        val source = tokens.firstOrNull() ?: return null
        return SqlTableReferenceFacts(
            range = sourceRange(segment.startOffset, segment.endOffset),
            name = source.text,
            alias = tokens.drop(1).aliasText(sourcePatterns),
            subquery = false,
        )
    }

    private fun SqlSourceScanner.joinFacts(
        range: OffsetRange,
        statementTokens: List<SqlToken>,
        references: List<SqlTableReferenceFacts>,
        sourcePatterns: SqlDialectSourcePatterns,
    ): List<SqlJoinFacts> =
        statementTokens
            .filter { token -> token.isKeyword("join") && parenthesisDepthAt(token.startOffset) == 0 }
            .mapNotNull { join ->
                val table = references.firstOrNull { reference -> offsetAt(reference.range.start) >= join.endOffset } ?: return@mapNotNull null
                SqlJoinFacts(
                    range = sourceRange(join.startOffset, offsetAt(table.range.end)),
                    kind = joinKindBefore(join, range.startOffset, statementTokens, sourcePatterns),
                    table = table,
                )
            }

    private fun joinKindBefore(
        join: SqlToken,
        statementStart: Int,
        statementTokens: List<SqlToken>,
        sourcePatterns: SqlDialectSourcePatterns,
    ): String {
        val modifiers =
            statementTokens
                .filter { token -> token.startOffset in statementStart..<join.startOffset }
                .takeLastWhile { token -> sourcePatterns.matches(JoinModifier, token) }
        return (modifiers.map { token -> token.text } + join.text).joinToString(" ")
    }

    private fun SqlSourceScanner.qualifiedReferences(
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
                    range = sourceRange(left.startOffset, right.endOffset),
                    qualifier = left.text,
                    name = right.text,
                )
            }

    private fun SqlSourceScanner.statementRanges(): Sequence<OffsetRange> =
        sequence {
            var start = 0
            characters.forEach { character ->
                if (character.value == ';') {
                    content.trimmedRange(start, character.offset + 1)?.let { range -> yield(range) }
                    start = character.offset + 1
                }
            }
            content.trimmedRange(start, content.length)?.let { range -> yield(range) }
        }

    private fun SqlSourceScanner.splitTopLevelSegments(
        startOffset: Int,
        endOffset: Int,
        depth: Int,
    ): List<OffsetRange> {
        val ranges = mutableListOf<OffsetRange>()
        var start = startOffset
        characters
            .asSequence()
            .dropWhile { character -> character.offset < startOffset }
            .takeWhile { character -> character.offset < endOffset }
            .forEach { character ->
                if (character.value == ',' && parenthesisDepthAt(character.offset) == depth) {
                    content.trimmedRange(start, character.offset)?.let(ranges::add)
                    start = character.offset + 1
                }
            }
        content.trimmedRange(start, endOffset)?.let(ranges::add)
        return ranges
    }

    private fun SqlSourceScanner.tokensIn(range: OffsetRange): List<SqlToken> {
        val allTokens = sqlTokens()
        var low = 0
        var high = allTokens.size
        while (low < high) {
            val middle = (low + high).ushr(1)
            if (allTokens[middle].startOffset < range.startOffset) {
                low = middle + 1
            } else {
                high = middle
            }
        }
        val tokens = mutableListOf<SqlToken>()
        for (index in low until allTokens.size) {
            val token = allTokens[index]
            if (token.startOffset >= range.endOffset) break
            tokens += token
        }
        return tokens
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

    private fun SqlSourceScanner.firstReferenceBoundaryAfter(
        tokens: List<SqlToken>,
        startIndex: Int,
        statementEnd: Int,
        sourcePatterns: SqlDialectSourcePatterns,
    ): Int {
        val lookahead =
            sourcePatterns
                .patternsFor(TableReferenceBoundary)
                .maxOfOrNull { pattern -> pattern.expression.parts.size }
                ?: 1
        return tokens
            .asSequence()
            .drop(startIndex)
            .withIndex()
            .firstOrNull { (relativeIndex, token) ->
                token.startOffset < statementEnd &&
                    parenthesisDepthAt(token.startOffset) == 0 &&
                    sourcePatterns.matches(
                        TableReferenceBoundary,
                        tokens.normalizedTextsFrom(startIndex + relativeIndex, lookahead),
                    )
            }?.value?.startOffset
            ?: statementEnd
    }
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

private fun SqlDialectSourcePatterns.matches(
    role: dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole,
    token: SqlToken,
): Boolean =
    matches(role, listOf(token.normalizedText))

private fun List<SqlToken>.normalizedTextsFrom(
    index: Int,
    limit: Int,
): List<String> =
    asSequence().drop(index).take(limit).map { token -> token.normalizedText }.toList()

private data class SqlCharacter(
    val value: Char,
    val offset: Int,
)

private class SqlSourceScanner(
    val content: String,
) {
    val characters: List<SqlCharacter> by lazy { content.sqlCharacters().toList() }

    private val tokens: List<SqlToken> by lazy { content.sqlTokens().toList() }

    private val parenthesisDepths: IntArray by lazy {
        IntArray(content.length + 1).also { depths ->
            var depth = 0
            var nextOffset = 0
            characters.forEach { character ->
                while (nextOffset <= character.offset) {
                    depths[nextOffset++] = depth
                }
                when (character.value) {
                    '(' -> depth++
                    ')' -> if (depth > 0) depth--
                }
            }
            while (nextOffset <= content.length) {
                depths[nextOffset++] = depth
            }
        }
    }

    private val matchingClosingParentheses: Map<Int, Int> by lazy {
        val openParentheses = java.util.ArrayDeque<Int>()
        buildMap {
            characters.forEach { character ->
                when (character.value) {
                    '(' -> openParentheses.addLast(character.offset)
                    ')' -> if (openParentheses.isNotEmpty()) put(openParentheses.removeLast(), character.offset)
                }
            }
        }
    }

    private val lineStarts: IntArray by lazy {
        val starts = mutableListOf(0)
        content.forEachIndexed { index, character ->
            if (character == '\n') starts += index + 1
        }
        starts.toIntArray()
    }

    fun sqlTokens(): List<SqlToken> = tokens

    fun parenthesisDepthAt(offset: Int): Int = parenthesisDepths[offset.coerceIn(0, content.length)]

    fun nextSqlCharacterAfter(offset: Int): SqlCharacter? {
        var low = 0
        var high = characters.size
        while (low < high) {
            val middle = (low + high).ushr(1)
            if (characters[middle].offset < offset) {
                low = middle + 1
            } else {
                high = middle
            }
        }
        for (index in low until characters.size) {
            val character = characters[index]
            if (!character.value.isWhitespace()) return character
        }
        return null
    }

    fun matchingClosingParenthesisOffset(openOffset: Int): Int? =
        if (content.getOrNull(openOffset) == '(') matchingClosingParentheses[openOffset] else null

    fun sourceRange(
        startOffset: Int,
        endOffset: Int,
    ): SourceRange =
        SourceRange(
            start = positionAt(startOffset),
            end = positionAt(endOffset),
        )

    fun offsetAt(position: SourcePosition): Int {
        if (position.line !in 1..lineStarts.size || position.column < 1) return content.length
        val offset = lineStarts[position.line - 1] + position.column - 1
        return if (offset in 0..content.length) offset else content.length
    }

    private fun positionAt(offset: Int): SourcePosition {
        val boundedOffset = offset.coerceIn(0, content.length)
        var low = 0
        var high = lineStarts.lastIndex
        while (low <= high) {
            val middle = (low + high).ushr(1)
            if (lineStarts[middle] <= boundedOffset) {
                low = middle + 1
            } else {
                high = middle - 1
            }
        }
        val lineStart = lineStarts[high.coerceAtLeast(0)]
        return SourcePosition(
            line = high.coerceAtLeast(0) + 1,
            column = boundedOffset - lineStart + 1,
        )
    }
}

private fun String.sqlTokens(): Sequence<SqlToken> =
    sequence {
        var index = 0
        while (index < length) {
            index =
                when {
                    startsWith("--", index) -> skipLineComment(index)
                    startsWith("/*", index) -> skipBlockComment(index)
                    this@sqlTokens[index] == '\'' -> skipSqlQuoted(index, '\'')
                    this@sqlTokens[index] == '"' -> skipSqlQuoted(index, '"')
                    this@sqlTokens[index] == '`' -> skipSqlQuoted(index, '`')
                    this@sqlTokens[index] == '[' -> skipSqlBracketQuoted(index)
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

private fun SqlToken.isKeyword(value: String): Boolean = text.equals(value, ignoreCase = true)

private fun Char.isIdentifierStart(): Boolean = this == '_' || isLetter()

private fun Char.isIdentifierPart(): Boolean = this == '_' || isLetterOrDigit()

private inline fun <T> measurePhase(
    noinline phaseTrace: ((AnalysisPhase, Long) -> Unit)?,
    phase: AnalysisPhase,
    block: () -> T,
): T {
    if (phaseTrace == null) return block()
    val start = System.nanoTime()
    return try {
        block()
    } finally {
        phaseTrace(phase, System.nanoTime() - start)
    }
}
