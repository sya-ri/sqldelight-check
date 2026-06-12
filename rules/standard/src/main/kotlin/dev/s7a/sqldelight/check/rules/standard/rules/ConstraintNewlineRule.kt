package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatterns
import dev.s7a.sqldelight.check.api.SqlDialectSourceTerm
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
            if (!token.isTerm(SqlDialectSourceTerm.Create)) return@forEachIndexed
            val table = tokens.getOrNull(index + 1)?.takeIf { it.isTerm(SqlDialectSourceTerm.Table) } ?: return@forEachIndexed
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
                .flatMap { item -> content.createTableConstraintTokens(item, itemDepth, context.database.dialect.sourcePatterns) }
                .forEach { constraint ->
                    val line = lines.lineContaining(constraint.startOffset) ?: return@forEach
                    if (line.firstNonWhitespaceOffset == constraint.startOffset) return@forEach
                    reporter.report(
                        RuleDiagnostic(
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
    sourcePatterns: SqlDialectSourcePatterns,
): List<SqlToken> {
    val itemTokens =
        sqlTokens()
            .dropWhile { token -> token.startOffset < item.startOffset }
            .takeWhile { token -> token.startOffset < item.endOffset }
            .filter { token -> sqlParenthesisDepthAt(token.startOffset) == itemDepth }
            .toList()
    val first = itemTokens.firstOrNull() ?: return emptyList()
    if (sourcePatterns.matches(SqlDialectSourcePatternRole.TableConstraintStart, itemTokens.normalizedTextsFrom(0))) {
        return listOf(first)
    }
    if (!substring(item.startOffset, item.endOffset).contains('\n')) return emptyList()
    return buildList {
        var index = 1
        while (index < itemTokens.size) {
            val token = itemTokens[index]
            val length =
                sourcePatterns.matchPrefix(SqlDialectSourcePatternRole.ColumnConstraintStart, itemTokens.normalizedTextsFrom(index))
            if (length != null && sqlParenthesisDepthAt(token.startOffset) == itemDepth) {
                add(token)
                index += length
            } else {
                index++
            }
        }
    }
}
