package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports multiline CREATE TABLE constraints that do not start their own line.
 */
public class ConstraintNewlineRule : Rule {
    override val id: RuleId = RuleId("constraint-newline")
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
            if (!token.isKeyword("create")) return@forEachIndexed
            val table = tokens.getOrNull(index + 1)?.takeIf { it.isKeyword("table") } ?: return@forEachIndexed
            val statementEnd = content.statementEndAfter(token.startOffset)
            val open =
                content.sqlCharacters()
                    .dropWhile { character -> character.offset < table.endOffset }
                    .takeWhile { character -> character.offset < statementEnd }
                    .firstOrNull { character -> character.value == '(' }
                    ?: return@forEachIndexed
            val close = content.matchingClosingParenthesisOffset(open.offset) ?: return@forEachIndexed
            val itemDepth = content.sqlParenthesisDepthAt(open.offset) + 1
            val items = content.commaSeparatedClauseItems(open.offset + 1, close, itemDepth)
            if (!content.isMultilineItemList(items)) return@forEachIndexed

            items
                .flatMap { item -> content.createTableConstraintTokens(item, itemDepth) }
                .forEach { constraint ->
                    val line = lines.lineContaining(constraint.startOffset) ?: return@forEach
                    if (line.firstNonWhitespaceOffset == constraint.startOffset) return@forEach
                    reporter.report(
                        Diagnostic(
                            ruleId = id,
                            severity = defaultSeverity,
                            message = "Multiline CREATE TABLE constraints should start their own line.",
                            file = context.file,
                            range = content.rangeAtOffsets(constraint.startOffset, constraint.endOffset),
                            database = context.database,
                        ),
                    )
                }
        }
    }
}

private fun String.createTableConstraintTokens(
    item: ClauseItem,
    itemDepth: Int,
): List<SqlToken> {
    val itemTokens =
        sqlTokens()
            .dropWhile { token -> token.startOffset < item.startOffset }
            .takeWhile { token -> token.startOffset < item.endOffset }
            .filter { token -> sqlParenthesisDepthAt(token.startOffset) == itemDepth }
            .toList()
    val first = itemTokens.firstOrNull() ?: return emptyList()
    if (first.normalizedText in tableConstraintStartKeywords) return listOf(first)
    if (!substring(item.startOffset, item.endOffset).contains('\n')) return emptyList()
    return itemTokens.drop(1).filter { token -> token.normalizedText in columnConstraintKeywords }
}

private val tableConstraintStartKeywords =
    setOf(
        "check",
        "constraint",
        "foreign",
        "primary",
        "unique",
    )

private val columnConstraintKeywords =
    setOf(
        "check",
        "collate",
        "constraint",
        "default",
        "generated",
        "not",
        "primary",
        "references",
        "unique",
    )
