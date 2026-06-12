package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SqlDialectSourceTerm
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports multiline INSERT column and VALUES lists that do not use one item per line.
 */
public class InsertValuesNewlineRule : Rule {
    override val id: RuleId = RuleId("insert-values-newline")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        val lines = content.linesWithRanges()
        val tokens = content.sqlTokens().toList()
        tokens.forEachIndexed { index, token ->
            if (!token.isTerm(SqlDialectSourceTerm.Insert)) return@forEachIndexed
            val statementEnd = content.statementEndAfter(token.startOffset)
            val values = tokens.firstTermAfter(index + 1, statementEnd, SqlDialectSourceTerm.Values) ?: return@forEachIndexed

            content.insertColumnListBetween(token.endOffset, values.startOffset)?.let { list ->
                content.reportMisalignedInsertList(list, lines, reporter, context)
            }

            val valuesDepth = content.sqlParenthesisDepthAt(values.startOffset)
            content.sqlCharacters()
                .dropWhile { character -> character.offset < values.endOffset }
                .takeWhile { character -> character.offset < statementEnd }
                .filter { character -> character.value == '(' && content.sqlParenthesisDepthAt(character.offset) == valuesDepth }
                .forEach { open ->
                    val close = content.matchingClosingParenthesisOffset(open.offset) ?: return@forEach
                    content.reportMisalignedInsertList(
                        ParenthesizedList(openOffset = open.offset, closeOffset = close),
                        lines,
                        reporter,
                        context,
                    )
                }
        }
    }

    private fun String.reportMisalignedInsertList(
        list: ParenthesizedList,
        lines: List<LineInfo>,
        reporter: DiagnosticReporter,
        context: RuleContext,
    ) {
        val itemDepth = sqlParenthesisDepthAt(list.openOffset) + 1
        val items = commaSeparatedClauseItems(list.openOffset + 1, list.closeOffset, itemDepth)
        if (!isMultilineItemList(items)) return
        if (lines.itemStartsAreOnOwnLines(items)) return
        reporter.report(
            RuleDiagnostic(
                severity = defaultSeverity,
                message = "Multiline INSERT column and VALUES lists should put each item on its own line.",
                file = context.file,
                range = rangeAtOffsets(list.openOffset, list.closeOffset + 1),
                database = context.database,
            ),
        )
    }
}

private data class ParenthesizedList(
    val openOffset: Int,
    val closeOffset: Int,
)

private fun String.insertColumnListBetween(
    startOffset: Int,
    endOffset: Int,
): ParenthesizedList? {
    val open =
        sqlCharacters()
            .dropWhile { character -> character.offset < startOffset }
            .takeWhile { character -> character.offset < endOffset }
            .firstOrNull { character -> character.value == '(' }
            ?: return null
    val close = matchingClosingParenthesisOffset(open.offset) ?: return null
    return if (close < endOffset) ParenthesizedList(openOffset = open.offset, closeOffset = close) else null
}
