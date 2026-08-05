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
        val parenthesisDepths = content.computeParenthesisDepths()
        content.createTableBodies(tokens, parenthesisDepths).forEach { body ->
            content.commaSeparatedClauseItems(body.openOffset + 1, body.closeOffset, body.itemDepth, parenthesisDepths)
                .mapNotNull { item -> content.compositePrimaryKeyToken(item, body.itemDepth, parenthesisDepths) }
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
    parenthesisDepths: IntArray,
): SqlToken? {
    val itemTokens =
        sqlTokens()
            .dropWhile { token -> token.startOffset < item.startOffset }
            .takeWhile { token -> token.startOffset < item.endOffset }
            .filter { token -> parenthesisDepths[token.startOffset] == itemDepth }
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
                it.value == '(' && parenthesisDepths[it.offset] == itemDepth
            }
            ?: return null
    val close = matchingClosingParenthesisOffset(open.offset)?.takeIf { it <= item.endOffset } ?: return null
    val columnDepth = parenthesisDepths[open.offset] + 1
    val hasMultipleColumns =
        sqlCharacters()
            .dropWhile { character -> character.offset <= open.offset }
            .takeWhile { character -> character.offset < close }
            .any { character -> character.value == ',' && parenthesisDepths[character.offset] == columnDepth }
    return if (hasMultipleColumns) primary else null
}
