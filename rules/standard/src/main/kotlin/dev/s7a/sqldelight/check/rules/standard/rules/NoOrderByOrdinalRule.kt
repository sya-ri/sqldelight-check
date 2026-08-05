package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole
import dev.s7a.sqldelight.check.api.SqlDialectSourceTerm
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports ordinal references in `GROUP BY` and `ORDER BY` clauses.
 */
public class NoOrderByOrdinalRule : Rule {
    override val id: RuleId = RuleId("no-order-by-ordinal")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        val tokens = content.sqlTokens().toList()
        val parenthesisDepths = content.computeParenthesisDepths()
        tokens.forEachIndexed { index, token ->
            val clause =
                when {
                    token.isTerm(SqlDialectSourceTerm.Group) -> OrdinalClause.GroupBy
                    token.isTerm(SqlDialectSourceTerm.Order) -> OrdinalClause.OrderBy
                    else -> return@forEachIndexed
                }
            val by = tokens.getOrNull(index + 1)?.takeIf { it.isTerm(SqlDialectSourceTerm.By) } ?: return@forEachIndexed
            if (parenthesisDepths[token.startOffset] != 0) return@forEachIndexed

            val boundaryRole =
                if (clause == OrdinalClause.GroupBy) {
                    SqlDialectSourcePatternRole.GroupByBoundary
                } else {
                    SqlDialectSourcePatternRole.OrderByBoundary
                }
            val statementEnd = content.statementEndAfter(token.startOffset)
            val clauseEnd =
                tokens.firstBoundaryOffsetAfter(index + 2, statementEnd, context.database.dialect.sourcePatterns, boundaryRole)
            content.topLevelOrdinalReferenceOffsets(by.endOffset, clauseEnd, clause)
                .forEach { ordinal ->
                    reporter.report(
                        RuleDiagnostic(
                            severity = defaultSeverity,
                            message = "${token.text.uppercase()} BY should reference columns by name instead of ordinal.",
                            file = context.file,
                            range = content.rangeAtOffsets(ordinal.startOffset, ordinal.endOffset),
                            database = context.database,
                        ),
                    )
                }
        }
    }
}

private enum class OrdinalClause {
    GroupBy,
    OrderBy,
}

private data class OrdinalReference(
    val startOffset: Int,
    val endOffset: Int,
    val number: Int,
)

private fun String.topLevelOrdinalReferenceOffsets(
    startOffset: Int,
    endOffset: Int,
    clause: OrdinalClause,
): List<OrdinalReference> =
    topLevelOrdinalItems(startOffset, endOffset)
        .mapNotNull { item ->
            val reference =
                if (clause == OrdinalClause.OrderBy) {
                    substring(item.startOffset, item.endOffset).withoutOrderByOrdinalSuffix()
                } else {
                    substring(item.startOffset, item.endOffset).trim()
                }
            val leadingWhitespace = substring(item.startOffset, item.endOffset).takeWhile { it.isWhitespace() }.length
            if (reference.matches(ordinalReferenceRegex)) {
                OrdinalReference(
                    startOffset = item.startOffset + leadingWhitespace,
                    endOffset = item.startOffset + leadingWhitespace + reference.length,
                    number = reference.toInt(),
                )
            } else {
                null
            }
        }

private data class OrdinalItem(
    val startOffset: Int,
    val endOffset: Int,
)

private fun String.topLevelOrdinalItems(
    startOffset: Int,
    endOffset: Int,
): List<OrdinalItem> {
    val items = mutableListOf<OrdinalItem>()
    var depth = 0
    var itemStart = startOffset
    sqlCharacters()
        .filter { character -> character.offset in startOffset until endOffset }
        .forEach { character ->
            when (character.value) {
                '(' -> depth++
                ')' -> if (depth > 0) depth--
                ',' -> {
                    if (depth == 0) {
                        items += OrdinalItem(itemStart, character.offset)
                        itemStart = character.offset + 1
                    }
                }
            }
        }
    items += OrdinalItem(itemStart, endOffset)
    return items
}

private fun String.withoutOrderByOrdinalSuffix(): String {
    var text = replace(ordinalHorizontalWhitespaceRegex, " ").trim()
    val words = text.split(' ')
    if (
        words.size >= 3 &&
        words[words.lastIndex - 1].equals(SqlDialectSourceTerm.Nulls.normalizedText, ignoreCase = true) &&
        words.last().isOrdinalNullsPlacement()
    ) {
        text = words.dropLast(2).joinToString(" ")
    }
    val directionWords = text.split(' ')
    if (directionWords.size >= 2 && directionWords.last().isOrdinalOrderDirection()) {
        text = directionWords.dropLast(1).joinToString(" ")
    }
    return text.trim()
}

private fun String.isOrdinalOrderDirection(): Boolean =
    equals(SqlDialectSourceTerm.Asc.normalizedText, ignoreCase = true) ||
        equals(SqlDialectSourceTerm.Desc.normalizedText, ignoreCase = true)

private fun String.isOrdinalNullsPlacement(): Boolean =
    equals(SqlDialectSourceTerm.First.normalizedText, ignoreCase = true) ||
        equals(SqlDialectSourceTerm.Last.normalizedText, ignoreCase = true)

private val ordinalReferenceRegex = Regex("[0-9]+")

private val ordinalHorizontalWhitespaceRegex = Regex("[ \\t\\r\\n]+")
