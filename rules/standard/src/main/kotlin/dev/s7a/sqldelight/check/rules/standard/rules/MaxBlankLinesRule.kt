package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.Fix
import dev.s7a.sqldelight.check.api.FixSafety
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.TextEdit
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports runs with more than one consecutive blank line.
 */
public class MaxBlankLinesRule : Rule {
    override val id: String = "max-blank-lines"
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        val lines = content.linesWithRanges()
        var index = 0
        while (index < lines.size) {
            if (!lines[index].text.isBlank()) {
                index++
                continue
            }

            val runStart = index
            while (index < lines.size && lines[index].text.isBlank()) {
                index++
            }
            val blankLineCount = index - runStart
            if (blankLineCount <= 1) continue

            val firstExtraLine = lines[runStart + 1]
            val lastExtraLine = lines[index - 1]
            val range = content.rangeAtOffsets(firstExtraLine.startOffset, lastExtraLine.newlineEndOffset)
            reporter.report(
                Diagnostic(
                    ruleId = RuleId(id),
                    severity = defaultSeverity,
                    message = "File should not contain more than one consecutive blank line.",
                    file = context.file,
                    range = range,
                    database = context.database,
                    fixes =
                        listOf(
                            Fix(
                                title = "Remove extra blank lines",
                                safety = FixSafety.Safe,
                                edits = listOf(TextEdit(range = range, replacement = "")),
                            ),
                        ),
                ),
            )
        }
    }
}
