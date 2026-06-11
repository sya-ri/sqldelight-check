package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.Fix
import dev.s7a.sqldelight.check.api.FixSafety
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SourceFileKind
import dev.s7a.sqldelight.check.api.TextEdit
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports adjacent top-level statements that are not separated by a blank line.
 */
public class BlankLineBetweenStatementsRule : Rule {
    override val id: String = "blank-line-between-statements"
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        val lines = content.linesWithRanges()
        val topLevelSemicolonOffsets = content.topLevelSemicolonOffsets()

        topLevelSemicolonOffsets.forEach { semicolonOffset ->
            val semicolonLineIndex = lines.indexOfLineContaining(semicolonOffset) ?: return@forEach
            val nextContentLineIndex =
                ((semicolonLineIndex + 1) until lines.size)
                    .firstOrNull { index -> lines[index].text.isNotBlank() }
                    ?: return@forEach

            val hasBlankLineBetween =
                ((semicolonLineIndex + 1) until nextContentLineIndex)
                    .any { index -> lines[index].text.isBlank() }
            if (hasBlankLineBetween) return@forEach

            val nextLine = lines[nextContentLineIndex]
            if (!nextLine.startsSqlDelightStatementOrLabel(context.file.kind)) return@forEach

            val range = content.rangeAtOffsets(nextLine.startOffset, nextLine.startOffset)
            reporter.report(
                Diagnostic(
                    ruleId = RuleId(id),
                    severity = defaultSeverity,
                    message = "Statements should be separated by one blank line.",
                    file = context.file,
                    range = range,
                    database = context.database,
                    fixes =
                        listOf(
                            Fix(
                                title = "Insert blank line between statements",
                                safety = FixSafety.Safe,
                                edits = listOf(TextEdit(range = range, replacement = "\n")),
                            ),
                        ),
                ),
            )
        }
    }
}

private fun String.topLevelSemicolonOffsets(): List<Int> {
    var depth = 0
    val offsets = mutableListOf<Int>()
    sqlCharacters().forEach { character ->
        when (character.value) {
            '(' -> depth++
            ')' -> depth = (depth - 1).coerceAtLeast(0)
            ';' -> if (depth == 0) offsets += character.offset
        }
    }
    return offsets
}

private fun List<LineInfo>.indexOfLineContaining(offset: Int): Int? =
    indexOfLast { line -> line.startOffset <= offset }
        .takeIf { index -> index >= 0 }

private fun LineInfo.startsSqlDelightStatementOrLabel(kind: SourceFileKind): Boolean {
    val trimmed = text.trimStart()
    if (trimmed.isEmpty()) return false
    if (kind == SourceFileKind.Query && sqlDelightStatementLabelRegex.matches(text)) return true
    val token = trimmed.takeWhile { character -> character == '_' || character.isLetterOrDigit() }
    return token.lowercase() in statementStartKeywordsForBlankLines
}

private val sqlDelightStatementLabelRegex = Regex("""\s*[A-Za-z_][A-Za-z0-9_]*\s*(?::|\{)\s*""")

private val statementStartKeywordsForBlankLines =
    setOf(
        "alter",
        "create",
        "delete",
        "drop",
        "insert",
        "select",
        "update",
        "with",
    )
