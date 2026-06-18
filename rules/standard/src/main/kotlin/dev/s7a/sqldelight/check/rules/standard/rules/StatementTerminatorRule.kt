package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SourceFileKind
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatterns
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.SqlDelightStatementStart
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.StatementContinuation
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.StatementStart
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports SQL statements that are not terminated by a semicolon.
 */
public class StatementTerminatorRule : Rule {
    override val id: RuleId = RuleId("statement-terminator")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        val sourcePatterns = context.database.dialect.sourcePatterns
        val starts =
            if (context.file.kind == SourceFileKind.Migration) {
                content.statementStarts(sourcePatterns, StatementStart)
            } else {
                content.statementStarts(sourcePatterns, SqlDelightStatementStart)
            }
        val labelOffsets =
            if (context.file.kind == SourceFileKind.Query) {
                content.sqlDelightLabelOffsets()
            } else {
                emptyList()
            }

        starts.forEachIndexed { index, start ->
            if (starts.isContinuationStart(index, sourcePatterns)) return@forEachIndexed

            val nextStartOffset = starts.nextBoundaryOffset(index, start, sourcePatterns)
            val nextLabelOffset = labelOffsets.firstOrNull { offset -> offset > start.offset } ?: content.length
            val boundaryOffset = minOf(nextStartOffset, nextLabelOffset)
            val lastSqlCharacter = content.lastSqlCharacterBefore(start.offset, boundaryOffset) ?: return@forEachIndexed
            if (lastSqlCharacter.value == ';') return@forEachIndexed

            reporter.report(
                RuleDiagnostic(
                    severity = defaultSeverity,
                    message = "Statement should be terminated by a semicolon.",
                    file = context.file,
                    range = content.rangeAtOffsets(lastSqlCharacter.offset, lastSqlCharacter.offset + 1),
                    database = context.database,
                    fixes = listOf(content.insertTokenFix(lastSqlCharacter.offset + 1, ";", "Insert statement terminator")),
                ),
            )
        }
    }
}

private data class DetectedStatementStart(
    val keyword: String,
    val offset: Int,
)

private fun String.statementStarts(
    sourcePatterns: SqlDialectSourcePatterns,
    role: dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole,
): List<DetectedStatementStart> {
    val tokens = sqlTokens().toList()
    val depths = topLevelDepthsByOffset()
    return tokens.asSequence()
        .filter { token -> sourcePatterns.matches(role, listOf(token.normalizedText)) }
        .filter { token -> depths[token.startOffset] == 0 }
        .filter { token -> isFirstSqlTokenOnLine(token.startOffset) }
        .map { token -> DetectedStatementStart(keyword = token.normalizedText, offset = token.startOffset) }
        .toList()
}

private fun String.topLevelDepthsByOffset(): Map<Int, Int> {
    val depths = mutableMapOf<Int, Int>()
    var depth = 0
    sqlCharacters().forEach { character ->
        when (character.value) {
            '(' -> {
                depths[character.offset] = depth
                depth++
            }
            ')' -> {
                depth = (depth - 1).coerceAtLeast(0)
                depths[character.offset] = depth
            }
            else -> depths[character.offset] = depth
        }
    }
    return depths
}

private fun String.isFirstSqlTokenOnLine(offset: Int): Boolean {
    var index = offset - 1
    while (index >= 0 && this[index] != '\n') {
        if (!this[index].isWhitespace()) return false
        index--
    }
    return true
}

private fun List<DetectedStatementStart>.nextBoundaryOffset(
    index: Int,
    current: DetectedStatementStart,
    sourcePatterns: SqlDialectSourcePatterns,
): Int {
    val next = asSequence().drop(index + 1).firstOrNull { start ->
        !current.hasContinuation(start, sourcePatterns)
    }
    return next?.offset ?: Int.MAX_VALUE
}

private fun List<DetectedStatementStart>.isContinuationStart(
    index: Int,
    sourcePatterns: SqlDialectSourcePatterns,
): Boolean {
    val current = this[index]
    val previous = getOrNull(index - 1) ?: return false
    return previous.hasContinuation(current, sourcePatterns)
}

private fun DetectedStatementStart.hasContinuation(
    next: DetectedStatementStart,
    sourcePatterns: SqlDialectSourcePatterns,
): Boolean =
    sourcePatterns.matches(StatementContinuation, listOf(keyword, next.keyword))

private fun String.lastSqlCharacterBefore(
    startOffset: Int,
    endOffset: Int,
): SqlCharacter? =
    sqlCharacters()
        .takeWhile { character -> character.offset < endOffset }
        .lastOrNull { character -> character.offset >= startOffset && !character.value.isWhitespace() }

private fun String.sqlDelightLabelOffsets(): List<Int> =
    linesWithRanges()
        .filter { line -> sqlDelightLabelRegex.matches(line.text) }
        .map { line -> line.startOffset }

private val sqlDelightLabelRegex = Regex("""\s*[A-Za-z_][A-Za-z0-9_]*\s*:\s*""")
