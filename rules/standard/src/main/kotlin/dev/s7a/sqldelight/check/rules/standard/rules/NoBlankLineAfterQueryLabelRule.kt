package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

import dev.s7a.sqldelight.check.api.RuleDiagnostic
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
 * Reports blank lines between a SQLDelight query label and its statement body.
 */
public class NoBlankLineAfterQueryLabelRule : Rule {
    override val id: RuleId = RuleId("no-blank-line-after-query-label")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        if (context.file.kind != SourceFileKind.Query) return

        val content = context.file.content
        val lines = content.linesWithRanges()
        lines.forEachIndexed { index, line ->
            if (!line.isSqlDelightQueryLabel()) return@forEachIndexed

            val firstBodyLineIndex =
                ((index + 1) until lines.size)
                    .firstOrNull { candidate -> lines[candidate].text.isNotBlank() }
                    ?: return@forEachIndexed
            if (firstBodyLineIndex == index + 1) return@forEachIndexed

            val firstBlankLine = lines[index + 1]
            val lastBlankLine = lines[firstBodyLineIndex - 1]
            val range = content.rangeAtOffsets(firstBlankLine.startOffset, lastBlankLine.newlineEndOffset)
            reporter.report(
                RuleDiagnostic(
                    severity = defaultSeverity,
                    message = "Query label should not be separated from its statement by blank lines.",
                    file = context.file,
                    range = range,
                    database = context.database,
                    fixes =
                        listOf(
                            Fix(
                                title = "Remove blank line after query label",
                                safety = FixSafety.Safe,
                                edits = listOf(TextEdit(range = range, replacement = "")),
                            ),
                        ),
                ),
            )
        }
    }
}

private fun LineInfo.isSqlDelightQueryLabel(): Boolean = sqlDelightQueryLabelRegex.matches(text)

private val sqlDelightQueryLabelRegex = Regex("""\s*[A-Za-z_][A-Za-z0-9_]*\s*[:{]\s*""")
