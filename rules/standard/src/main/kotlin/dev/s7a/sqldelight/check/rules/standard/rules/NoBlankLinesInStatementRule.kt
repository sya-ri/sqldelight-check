package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.Fix
import dev.s7a.sqldelight.check.api.FixSafety
import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.TextEdit
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext
import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

/**
 * Reports blank lines inside a SQL statement.
 */
public class NoBlankLinesInStatementRule : Rule {
    override val id: RuleId = RuleId("no-blank-lines-in-statement")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        val lines = content.linesWithRanges()
        var statementStartOffset = 0

        content.topLevelSemicolonOffsets().forEach { semicolonOffset ->
            val statementLines = lines.statementLines(statementStartOffset, semicolonOffset)
            statementLines.reportBlankLineRuns(content, context, reporter, defaultSeverity)
            statementStartOffset = semicolonOffset + 1
        }
    }
}

private fun List<LineInfo>.statementLines(
    startOffset: Int,
    semicolonOffset: Int,
): List<LineInfo> {
    val semicolonLine = lineContaining(semicolonOffset) ?: return emptyList()
    val firstContentLine =
        firstOrNull { line ->
            line.hasContentAtOrAfter(startOffset) &&
                line.startOffset <= semicolonOffset &&
                line.text.isNotBlank() &&
                !line.isSqlDelightLabel()
        } ?: return emptyList()
    return filter { line ->
        line.number >= firstContentLine.number &&
            line.number <= semicolonLine.number
    }
}

private fun List<LineInfo>.reportBlankLineRuns(
    content: String,
    context: RuleContext,
    reporter: DiagnosticReporter,
    severity: Severity,
) {
    var index = 1
    while (index < lastIndex) {
        if (!this[index].text.isBlank()) {
            index++
            continue
        }

        val runStart = index
        while (index < lastIndex && this[index].text.isBlank()) {
            index++
        }

        val firstBlankLine = this[runStart]
        val lastBlankLine = this[index - 1]
        val range = content.rangeAtOffsets(firstBlankLine.startOffset, lastBlankLine.newlineEndOffset)
        reporter.report(
            RuleDiagnostic(
                severity = severity,
                message = "Statement should not contain blank lines.",
                file = context.file,
                range = range,
                database = context.database,
                fixes =
                    listOf(
                        Fix(
                            title = "Remove blank lines inside statement",
                            safety = FixSafety.Safe,
                            edits = listOf(TextEdit(range = range, replacement = "")),
                        ),
                    ),
            ),
        )
    }
}

private fun LineInfo.isSqlDelightLabel(): Boolean = sqlDelightLabelRegex.matches(text)

private fun LineInfo.hasContentAtOrAfter(offset: Int): Boolean {
    if (endOffset <= offset) return false
    val relativeOffset = (offset - startOffset).coerceAtLeast(0)
    return text.drop(relativeOffset).isNotBlank()
}

private val sqlDelightLabelRegex = Regex("""\s*[A-Za-z_][A-Za-z0-9_]*\s*[:{]\s*""")
