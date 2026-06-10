package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports `ORDER BY` clauses that mix explicit and implicit sort directions.
 */
public class ConsistentOrderByDirectionRule : Rule {
    override val id: RuleId = RuleId("standard:consistent-order-by-direction")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnablement: Enablement = Enablement.Auto

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        val tokens = content.sqlTokens().toList()
        tokens.forEachIndexed { index, token ->
            if (!token.text.equals("order", ignoreCase = true)) return@forEachIndexed
            val by = tokens.getOrNull(index + 1)?.takeIf { it.text.equals("by", ignoreCase = true) }
                ?: return@forEachIndexed
            val clauseEnd = tokens.orderByClauseEnd(index + 2, content.statementEndAfter(token.startOffset))
            val itemDirections = content.orderByItemDirections(by.endOffset, clauseEnd)
            if (itemDirections.size < 2 || itemDirections.all { it } || itemDirections.none { it }) {
                return@forEachIndexed
            }

            reporter.report(
                Diagnostic(
                    ruleId = id,
                    severity = defaultSeverity,
                    message = "ORDER BY should specify ASC/DESC for all columns or for none of them.",
                    file = context.file,
                    range = content.rangeAtOffsets(token.startOffset, by.endOffset),
                    database = context.database,
                ),
            )
        }
    }
}

// FIXME: Replace this source-text clause slicing with SQLDelight-derived ORDER BY expression facts.
private fun List<SqlToken>.orderByClauseEnd(
    startIndex: Int,
    statementEnd: Int,
): Int =
    asSequence()
        .drop(startIndex)
        .firstOrNull { token -> token.startOffset < statementEnd && token.text.lowercase() in orderByBoundaryKeywords }
        ?.startOffset
        ?: statementEnd

private fun String.orderByItemDirections(
    startOffset: Int,
    endOffset: Int,
): List<Boolean> {
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
        .map { (start, end) -> tokens.filter { token -> token.startOffset >= start && token.endOffset <= end } }
        .filter { itemTokens -> itemTokens.isNotEmpty() }
        .map { itemTokens -> itemTokens.any { token -> token.text.lowercase() in setOf("asc", "desc") } }
}

private val orderByBoundaryKeywords =
    setOf(
        "fetch",
        "limit",
        "offset",
        "union",
        "where",
    )
