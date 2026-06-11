package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.Fix
import dev.s7a.sqldelight.check.api.FixSafety
import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SourceFileKind
import dev.s7a.sqldelight.check.api.TextEdit
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext
import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

/**
 * Normalizes leading indentation in multiline SQL statements.
 */
public class StatementIndentationRule : Rule {
    override val id: RuleId = RuleId("statement-indentation")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        content.indentationFindings(context.file.kind).forEach { finding ->
            val range = content.rangeAtOffsets(finding.startOffset, finding.endOffset)
            reporter.report(
                RuleDiagnostic(
                    severity = defaultSeverity,
                    message = "Statement indentation should use ${finding.expectedWidth} leading spaces.",
                    file = context.file,
                    range = range,
                    database = context.database,
                    fixes =
                        listOf(
                            Fix(
                                title = "Normalize statement indentation",
                                safety = FixSafety.Safe,
                                edits = listOf(TextEdit(range = range, replacement = " ".repeat(finding.expectedWidth))),
                            ),
                        ),
                ),
            )
        }
    }
}

private data class IndentationFinding(
    val startOffset: Int,
    val endOffset: Int,
    val expectedWidth: Int,
)

private enum class LayoutSection {
    SelectList,
    Other,
}

private fun String.indentationFindings(kind: SourceFileKind): List<IndentationFinding> {
    val findings = mutableListOf<IndentationFinding>()
    val eligibleLines = multilineStatementLineNumbers(kind)
    val sections = mutableMapOf<Int, LayoutSection>()
    val openParenthesisIndents = mutableListOf<Int>()

    linesWithRanges().forEach { line ->
        val firstContentOffset = line.firstNonWhitespaceOffset
        if (firstContentOffset == null) {
            return@forEach
        }

        val trimmed = line.text.substring(firstContentOffset - line.startOffset)
        val leadingClosingParentheses = trimmed.leadingClosingParentheses()
        val baseIndent = openParenthesisIndents.baseIndent(leadingClosingParentheses)
        val firstKeyword = trimmed.firstKeyword()
        val expectedWidth =
            if (line.isSqlDelightLabel(kind)) {
                0
            } else {
                baseIndent.indentWidthFor(
                    firstKeyword = firstKeyword,
                    section = sections[baseIndent],
                    trimmed = trimmed,
                    isParenthesized = openParenthesisIndents.isNotEmpty(),
                )
            }
        val actualWidth = firstContentOffset - line.startOffset
        if (line.number in eligibleLines && actualWidth != expectedWidth) {
            findings +=
                IndentationFinding(
                    startOffset = line.startOffset,
                    endOffset = firstContentOffset,
                    expectedWidth = expectedWidth,
                )
        }

        sections.updateSection(baseIndent, firstKeyword)
        updateParenthesisIndents(line, expectedWidth, openParenthesisIndents)
    }
    return findings
}

private fun List<Int>.baseIndent(leadingClosingParentheses: Int): Int {
    if (leadingClosingParentheses > 0) {
        val closingIndex = size - leadingClosingParentheses
        return if (closingIndex >= 0) {
            this[closingIndex]
        } else {
            0
        }
    }
    return if (isNotEmpty()) {
        last() + indentUnit
    } else {
        0
    }
}

private fun Int.indentWidthFor(
    firstKeyword: String?,
    section: LayoutSection?,
    trimmed: String,
    isParenthesized: Boolean,
): Int {
    return when {
        firstKeyword in parenthesizedContinuationKeywords && isParenthesized -> this
        firstKeyword in majorClauseKeywords -> this
        firstKeyword in continuationKeywords -> this + indentUnit
        trimmed.startsWith(")") && section == LayoutSection.SelectList -> this + indentUnit
        section == LayoutSection.SelectList -> this + indentUnit
        else -> this
    }
}

private fun String.multilineStatementLineNumbers(kind: SourceFileKind): Set<Int> {
    val eligibleLineNumbers = mutableSetOf<Int>()
    val currentStatementLines = mutableListOf<LineInfo>()
    linesWithRanges().forEach { line ->
        if (line.hasSqlContent(kind)) {
            currentStatementLines += line
        }
        if (line.hasStatementTerminator()) {
            eligibleLineNumbers += currentStatementLines.eligibleLineNumbers()
            currentStatementLines.clear()
        }
    }
    eligibleLineNumbers += currentStatementLines.eligibleLineNumbers()
    return eligibleLineNumbers
}

private fun LineInfo.hasSqlContent(kind: SourceFileKind): Boolean =
    firstNonWhitespaceOffset != null && !isSqlDelightLabel(kind)

private fun LineInfo.hasStatementTerminator(): Boolean =
    text.sqlCharacters().any { character -> character.value == ';' }

private fun List<LineInfo>.eligibleLineNumbers(): List<Int> =
    if (size > 1) {
        map { line -> line.number }
    } else {
        emptyList()
    }

private fun updateParenthesisIndents(
    line: LineInfo,
    lineIndent: Int,
    openParenthesisIndents: MutableList<Int>,
) {
    line.text.sqlCharacters().forEach { character ->
        when (character.value) {
            '(' -> openParenthesisIndents += lineIndent
            ')' -> if (openParenthesisIndents.isNotEmpty()) {
                openParenthesisIndents.removeAt(openParenthesisIndents.lastIndex)
            }
        }
    }
}

private fun MutableMap<Int, LayoutSection>.updateSection(
    depth: Int,
    firstKeyword: String?,
) {
    when (firstKeyword) {
        "select" -> this[depth] = LayoutSection.SelectList
        in sectionBoundaryKeywords -> this[depth] = LayoutSection.Other
    }
}

private fun String.leadingClosingParentheses(): Int =
    takeWhile { character -> character == ')' }.length

private fun LineInfo.isSqlDelightLabel(kind: SourceFileKind): Boolean =
    kind == SourceFileKind.Query && sqlDelightLabelRegex.matches(text)

private fun String.firstKeyword(): String? {
    val start = indexOfFirst { character -> character.isIdentifierStart() }
    if (start == -1) return null
    var end = start + 1
    while (end < length && this[end].isIdentifierPart()) {
        end++
    }
    return substring(start, end).lowercase()
}

private val sqlDelightLabelRegex = Regex("""\s*[A-Za-z_][A-Za-z0-9_]*\s*[:{]\s*""")

private const val indentUnit = 2

private val majorClauseKeywords =
    setOf(
        "alter",
        "create",
        "delete",
        "from",
        "group",
        "having",
        "insert",
        "join",
        "left",
        "limit",
        "offset",
        "order",
        "returning",
        "right",
        "select",
        "set",
        "union",
        "update",
        "values",
        "where",
        "with",
    )

private val sectionBoundaryKeywords =
    majorClauseKeywords - "select" + setOf("by", "into")

private val continuationKeywords =
    setOf(
        "and",
        "else",
        "or",
        "over",
        "partition",
        "range",
        "rows",
        "then",
        "when",
    )

private val parenthesizedContinuationKeywords =
    setOf(
        "or",
        "order",
        "partition",
        "range",
        "rows",
    )
