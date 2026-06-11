package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SourceFileKind
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
        val starts =
            if (context.file.kind == SourceFileKind.Migration) {
                content.statementStarts()
            } else {
                content.statementStarts().filter { start -> start.keyword in sqlDelightStatementStartKeywords }
            }
        val labelOffsets =
            if (context.file.kind == SourceFileKind.Query) {
                content.sqlDelightLabelOffsets()
            } else {
                emptyList()
            }

        starts.forEachIndexed { index, start ->
            if (starts.isContinuationStart(index)) return@forEachIndexed

            val nextStartOffset = starts.nextBoundaryOffset(index, start)
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
                    fixes = emptyList(),
                ),
            )
        }
    }
}

private data class StatementStart(
    val keyword: String,
    val offset: Int,
)

private fun String.statementStarts(): List<StatementStart> {
    val tokens = sqlTokens().toList()
    val depths = topLevelDepthsByOffset()
    return tokens
        .filter { token -> token.normalizedText in statementStartKeywords }
        .filter { token -> depths[token.startOffset] == 0 }
        .filter { token -> isFirstSqlTokenOnLine(token.startOffset) }
        .filterNot { token -> token.normalizedText == "create" && isCreateTrigger(tokens, token) }
        .map { token -> StatementStart(keyword = token.normalizedText, offset = token.startOffset) }
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

private fun isCreateTrigger(
    tokens: List<SqlToken>,
    createToken: SqlToken,
): Boolean {
    val createIndex = tokens.indexOf(createToken)
    val nextToken = tokens.getOrNull(createIndex + 1) ?: return false
    return nextToken.isKeyword("trigger")
}

private fun List<StatementStart>.nextBoundaryOffset(
    index: Int,
    current: StatementStart,
): Int {
    val next = asSequence().drop(index + 1).firstOrNull { start ->
        !current.hasContinuation(start)
    }
    return next?.offset ?: Int.MAX_VALUE
}

private fun List<StatementStart>.isContinuationStart(index: Int): Boolean {
    val current = this[index]
    val previous = getOrNull(index - 1) ?: return false
    return previous.hasContinuation(current)
}

private fun StatementStart.hasContinuation(next: StatementStart): Boolean {
    val continuationKeywords = statementContinuationKeywords[keyword] ?: return false
    return next.keyword in continuationKeywords
}

private fun String.lastSqlCharacterBefore(
    startOffset: Int,
    endOffset: Int,
): SqlCharacter? =
    sqlCharacters()
        .takeWhile { character -> character.offset < endOffset }
        .filter { character -> character.offset >= startOffset && !character.value.isWhitespace() }
        .lastOrNull()

private fun String.sqlDelightLabelOffsets(): List<Int> =
    linesWithRanges()
        .filter { line -> sqlDelightLabelRegex.matches(line.text) }
        .map { line -> line.startOffset }

private val sqlDelightLabelRegex = Regex("""\s*[A-Za-z_][A-Za-z0-9_]*\s*:\s*""")

private val statementStartKeywords =
    setOf(
        "alter",
        "create",
        "delete",
        "drop",
        "insert",
        "select",
        "update",
        "with",
    )

private val sqlDelightStatementStartKeywords = statementStartKeywords

private val statementContinuationKeywords =
    mapOf(
        "create" to setOf("select", "with"),
        "insert" to setOf("select", "with"),
        "with" to setOf("delete", "insert", "select", "update"),
    )
