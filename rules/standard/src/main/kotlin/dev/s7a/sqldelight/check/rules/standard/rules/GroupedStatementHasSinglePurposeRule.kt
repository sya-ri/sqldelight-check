package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SourceFileKind
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports SQLDelight grouped statements that mix read and write statements.
 */
public class GroupedStatementHasSinglePurposeRule : Rule {
    override val id: String = "grouped-statement-has-single-purpose"
    override val defaultSeverity: Severity = Severity.Info
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        if (context.file.kind != SourceFileKind.Query) return

        val content = context.file.content
        content.groupedStatementRanges().forEach { group ->
            val verbs =
                content
                    .sqlTokens()
                    .filter { token -> token.startOffset in group.bodyStartOffset until group.bodyEndOffset }
                    .filter { token -> content.sqlParenthesisDepthAt(token.startOffset) == group.bodyDepth }
                    .map { token -> token.normalizedText }
                    .filter { token -> token in groupedStatementVerbs }
                    .toSet()
            if ("select" !in verbs || verbs.none { verb -> verb in writeStatementVerbs }) return@forEach

            reporter.report(
                Diagnostic(
                    ruleId = RuleId(id),
                    severity = defaultSeverity,
                    message = "Grouped SQLDelight statements should not mix read and write statements.",
                    file = context.file,
                    range = content.rangeAtOffsets(group.nameStartOffset, group.nameEndOffset),
                    database = context.database,
                ),
            )
        }
    }
}

private data class GroupedStatementRange(
    val nameStartOffset: Int,
    val nameEndOffset: Int,
    val bodyStartOffset: Int,
    val bodyEndOffset: Int,
    val bodyDepth: Int,
)

private val groupedStatementVerbs = setOf("delete", "insert", "select", "update")

private val writeStatementVerbs = setOf("delete", "insert", "update")

private fun String.groupedStatementRanges(): Sequence<GroupedStatementRange> =
    sequence {
        linesWithRanges().forEach { line ->
            val first = line.firstNonWhitespaceOffset ?: return@forEach
            val name = identifierTokenAt(first) ?: return@forEach
            val openOffset = nextNonHorizontalWhitespaceOffset(name.endOffset) ?: return@forEach
            if (getOrNull(openOffset) != '{') return@forEach
            val closeOffset = matchingClosingBraceOffset(openOffset) ?: return@forEach
            yield(
                GroupedStatementRange(
                    nameStartOffset = name.startOffset,
                    nameEndOffset = name.endOffset,
                    bodyStartOffset = openOffset + 1,
                    bodyEndOffset = closeOffset,
                    bodyDepth = sqlParenthesisDepthAt(openOffset),
                ),
            )
        }
    }

private fun String.matchingClosingBraceOffset(openOffset: Int): Int? {
    var depth = 0
    sqlCharacters()
        .dropWhile { character -> character.offset < openOffset }
        .forEach { character ->
            when (character.value) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return character.offset
                }
            }
        }
    return null
}
