package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports `GROUP BY` and `ORDER BY` clauses that mix ordinal and named references.
 */
public class ConsistentColumnReferencesRule : Rule {
    override val id: RuleId = RuleId("consistent-column-references")
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
            val by = tokens.getOrNull(index + 1)?.takeIf { it.isKeyword("by") }
                ?: return@forEachIndexed
            if (content.parenthesisDepthAt(token.startOffset) != 0) return@forEachIndexed

            val clauseName = "${token.text.uppercase()} ${by.text.uppercase()}"
            val boundaryKeywords =
                if (token.isKeyword("group")) groupByReferenceBoundaryKeywords else orderByReferenceBoundaryKeywords
            val statementEnd = content.statementEndAfter(token.startOffset)
            val clauseEnd = tokens.firstBoundaryOffsetAfter(index + 2, statementEnd, boundaryKeywords)
            val references = content.columnReferenceKinds(by.endOffset, clauseEnd, token.normalizedText)
            if (
                references.size < 2 ||
                references.all { it == ColumnReferenceKind.Ordinal } ||
                references.all { it == ColumnReferenceKind.Named }
            ) {
                return@forEachIndexed
            }

            reporter.report(
                RuleDiagnostic(
                    severity = defaultSeverity,
                    message =
                        "$clauseName should not mix ordinal column references " +
                            "with named or expression references.",
                    file = context.file,
                    range = content.rangeAtOffsets(token.startOffset, by.endOffset),
                    database = context.database,
                ),
            )
        }
    }
}

private enum class ColumnReferenceKind {
    Ordinal,
    Named,
}

private data class ColumnReferenceItem(
    val startOffset: Int,
    val endOffset: Int,
)

private fun String.columnReferenceKinds(
    startOffset: Int,
    endOffset: Int,
    clauseKeyword: String,
): List<ColumnReferenceKind> =
    topLevelColumnReferenceItems(startOffset, endOffset)
        .mapNotNull { item -> columnReferenceKind(item, clauseKeyword) }

private fun String.topLevelColumnReferenceItems(
    startOffset: Int,
    endOffset: Int,
): List<ColumnReferenceItem> {
    val items = mutableListOf<ColumnReferenceItem>()
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
                        items += ColumnReferenceItem(itemStart, character.offset)
                        itemStart = character.offset + 1
                    }
                }
            }
        }
    items += ColumnReferenceItem(itemStart, endOffset)
    return items
}

private fun String.columnReferenceKind(
    item: ColumnReferenceItem,
    clauseKeyword: String,
): ColumnReferenceKind? {
    val text = substring(item.startOffset, item.endOffset).trim()
    if (text.isEmpty()) return null
    val reference =
        if (clauseKeyword == "order") {
            text.withoutOrderBySuffix()
        } else {
            text
        }
    return if (reference.matches(ordinalReferenceRegex)) ColumnReferenceKind.Ordinal else ColumnReferenceKind.Named
}

private fun String.withoutOrderBySuffix(): String {
    var text = replace(horizontalWhitespaceRegex, " ").trim()
    val words = text.split(' ')
    if (
        words.size >= 3 &&
        words[words.lastIndex - 1].equals("nulls", ignoreCase = true) &&
        words.last().isNullsPlacement()
    ) {
        text = words.dropLast(2).joinToString(" ")
    }
    val directionWords = text.split(' ')
    if (directionWords.size >= 2 && directionWords.last().isOrderDirection()) {
        text = directionWords.dropLast(1).joinToString(" ")
    }
    return text.trim()
}

private fun String.parenthesisDepthAt(offset: Int): Int {
    var depth = 0
    sqlCharacters()
        .takeWhile { character -> character.offset < offset }
        .forEach { character ->
            when (character.value) {
                '(' -> depth++
                ')' -> if (depth > 0) depth--
            }
        }
    return depth
}

private fun String.isOrderDirection(): Boolean = equals("asc", ignoreCase = true) || equals("desc", ignoreCase = true)

private fun String.isNullsPlacement(): Boolean = equals("first", ignoreCase = true) || equals("last", ignoreCase = true)

private val ordinalReferenceRegex = Regex("[0-9]+")

private val horizontalWhitespaceRegex = Regex("[ \\t\\r\\n]+")

private val groupByReferenceBoundaryKeywords =
    setOf(
        "except",
        "having",
        "intersect",
        "limit",
        "offset",
        "order",
        "union",
        "where",
        "window",
    )

private val orderByReferenceBoundaryKeywords =
    setOf(
        "except",
        "fetch",
        "intersect",
        "limit",
        "offset",
        "union",
        "where",
    )
