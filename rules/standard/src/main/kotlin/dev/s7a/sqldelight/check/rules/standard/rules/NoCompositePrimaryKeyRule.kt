package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SqlDialectSourceTerm
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext
import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

/**
 * Reports CREATE TABLE constraints that declare composite primary keys.
 */
public class NoCompositePrimaryKeyRule : Rule {
    override val id: RuleId = RuleId("no-composite-primary-key")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = false

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
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

            content.commaSeparatedClauseItems(open.offset + 1, close, itemDepth)
                .mapNotNull { item -> content.compositePrimaryKeyToken(item, itemDepth) }
                .forEach { primary ->
                    reporter.report(
                        RuleDiagnostic(
                            severity = defaultSeverity,
                            message = "Avoid composite primary keys and use a single-column primary key.",
                            file = context.file,
                            range = content.rangeAtOffsets(primary.startOffset, primary.endOffset),
                            database = context.database,
                        ),
                    )
                }
        }
    }
}

private fun String.compositePrimaryKeyToken(
    item: ClauseItem,
    itemDepth: Int,
): SqlToken? {
    val itemTokens =
        sqlTokens()
            .dropWhile { token -> token.startOffset < item.startOffset }
            .takeWhile { token -> token.startOffset < item.endOffset }
            .filter { token -> sqlParenthesisDepthAt(token.startOffset) == itemDepth }
            .toList()
    val primary =
        itemTokens
            .zipWithNext()
            .firstOrNull { (left, right) ->
                left.isTerm(SqlDialectSourceTerm.Primary) && right.normalizedText == "key"
            }
            ?.first
            ?: return null
    val key = itemTokens.getOrNull(itemTokens.indexOf(primary) + 1) ?: return null
    val open =
        sqlCharacters()
            .dropWhile { character -> character.offset < key.endOffset }
            .takeWhile { character -> character.offset < item.endOffset }
            .firstOrNull {
                it.value == '(' && sqlParenthesisDepthAt(it.offset) == itemDepth
            }
            ?: return null
    val close = matchingClosingParenthesisOffset(open.offset)?.takeIf { it <= item.endOffset } ?: return null
    val columnDepth = sqlParenthesisDepthAt(open.offset) + 1
    val hasMultipleColumns =
        sqlCharacters()
            .dropWhile { character -> character.offset <= open.offset }
            .takeWhile { character -> character.offset < close }
            .any { character -> character.value == ',' && sqlParenthesisDepthAt(character.offset) == columnDepth }
    return if (hasMultipleColumns) primary else null
}
