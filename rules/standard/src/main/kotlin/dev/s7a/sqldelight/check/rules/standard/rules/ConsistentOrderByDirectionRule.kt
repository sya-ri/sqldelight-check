package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

import dev.s7a.sqldelight.check.api.Fix
import dev.s7a.sqldelight.check.api.FixSafety
import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.ClauseBoundary
import dev.s7a.sqldelight.check.api.SqlDialectSourceTerm
import dev.s7a.sqldelight.check.api.TextEdit
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports `ORDER BY` clauses that mix explicit and implicit sort directions.
 */
public class ConsistentOrderByDirectionRule : Rule {
    override val id: RuleId = RuleId("consistent-order-by-direction")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        val tokens = content.sqlTokens().toList()
        tokens.forEachIndexed { index, token ->
            if (!token.isTerm(SqlDialectSourceTerm.Order)) return@forEachIndexed
            val by = tokens.getOrNull(index + 1)?.takeIf { it.isTerm(SqlDialectSourceTerm.By) }
                ?: return@forEachIndexed
            val clauseEnd =
                tokens.firstBoundaryOffsetAfter(
                    index + 2,
                    content.statementEndAfter(token.startOffset),
                    context.database.dialect.sourcePatterns,
                    ClauseBoundary,
                )
            val items = content.orderByItems(by.endOffset, clauseEnd)
            if (items.size < 2 || items.all { it.hasDirection } || items.none { it.hasDirection }) {
                return@forEachIndexed
            }

            reporter.report(
                RuleDiagnostic(
                    severity = defaultSeverity,
                    message = "ORDER BY should specify ASC/DESC for all columns or for none of them.",
                    file = context.file,
                    range = content.rangeAtOffsets(token.startOffset, by.endOffset),
                    database = context.database,
                    fixes = listOf(content.addImplicitAscFix(items)),
                ),
            )
        }
    }
}

private data class OrderByItem(
    val startOffset: Int,
    val endOffset: Int,
    val hasDirection: Boolean,
)

private fun String.orderByItems(
    startOffset: Int,
    endOffset: Int,
): List<OrderByItem> {
    val commaOffsets =
        sqlCharacters()
            .filter { character -> character.offset in startOffset until endOffset && character.value == ',' }
            .map { character -> character.offset }
            .toList()
    val ranges = buildList {
        var start = startOffset
        commaOffsets.forEach { commaOffset ->
            add(start to commaOffset)
            start = commaOffset + 1
        }
        add(start to endOffset)
    }
    val tokens = sqlTokens().toList()
    return ranges
        .map { (start, end) -> start to tokens.filter { token -> token.startOffset >= start && token.endOffset <= end } }
        .filter { (_, itemTokens) -> itemTokens.isNotEmpty() }
        .map { (start, itemTokens) ->
            OrderByItem(
                startOffset = start,
                endOffset = itemTokens.last().endOffset,
                hasDirection = itemTokens.any { token -> token.isTerm(SqlDialectSourceTerm.Asc) || token.isTerm(SqlDialectSourceTerm.Desc) },
            )
        }
}

private fun String.addImplicitAscFix(items: List<OrderByItem>): Fix =
    Fix(
        title = "Add ASC to implicit ORDER BY directions",
        safety = FixSafety.Safe,
        edits =
            items
                .filterNot { item -> item.hasDirection }
                .map { item -> TextEdit(range = rangeAtOffsets(item.endOffset, item.endOffset), replacement = " ASC") },
    )
