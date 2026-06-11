package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
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
        tokens.forEachIndexed { index, token ->
            if (!token.isKeyword("group") && !token.isKeyword("order")) return@forEachIndexed
            val by = tokens.getOrNull(index + 1)?.takeIf { it.isKeyword("by") } ?: return@forEachIndexed
            val clauseDepth = content.sqlParenthesisDepthAt(token.startOffset)
            if (clauseDepth != 0) return@forEachIndexed

            val boundaryKeywords =
                if (token.isKeyword("group")) groupByOrdinalBoundaryKeywords else orderByOrdinalBoundaryKeywords
            val statementEnd = content.statementEndAfter(token.startOffset)
            val clauseEnd = tokens.firstBoundaryOffsetAfter(index + 2, statementEnd, boundaryKeywords)
            content.topLevelOrdinalReferenceOffsets(by.endOffset, clauseEnd, token.normalizedText)
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

private data class OrdinalReference(
    val startOffset: Int,
    val endOffset: Int,
)

private fun String.topLevelOrdinalReferenceOffsets(
    startOffset: Int,
    endOffset: Int,
    clauseKeyword: String,
): List<OrdinalReference> =
    topLevelOrdinalItems(startOffset, endOffset)
        .mapNotNull { item ->
            val reference =
                if (clauseKeyword == "order") {
                    substring(item.startOffset, item.endOffset).withoutOrderByOrdinalSuffix()
                } else {
                    substring(item.startOffset, item.endOffset).trim()
                }
            val leadingWhitespace = substring(item.startOffset, item.endOffset).takeWhile { it.isWhitespace() }.length
            if (reference.matches(ordinalReferenceRegex)) {
                OrdinalReference(item.startOffset + leadingWhitespace, item.startOffset + leadingWhitespace + reference.length)
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
        words[words.lastIndex - 1].equals("nulls", ignoreCase = true) &&
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

private fun String.isOrdinalOrderDirection(): Boolean = equals("asc", ignoreCase = true) || equals("desc", ignoreCase = true)

private fun String.isOrdinalNullsPlacement(): Boolean = equals("first", ignoreCase = true) || equals("last", ignoreCase = true)

private val ordinalReferenceRegex = Regex("[0-9]+")

private val ordinalHorizontalWhitespaceRegex = Regex("[ \\t\\r\\n]+")

private val groupByOrdinalBoundaryKeywords =
    setOf("except", "having", "intersect", "limit", "offset", "order", "union", "where", "window")

private val orderByOrdinalBoundaryKeywords = setOf("except", "fetch", "intersect", "limit", "offset", "union", "where")
