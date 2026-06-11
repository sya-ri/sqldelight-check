package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.Fix
import dev.s7a.sqldelight.check.api.FixSafety
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.TextEdit
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports blank lines after the last SQLDelight declaration or statement.
 */
public class NoTrailingBlankLinesRule : Rule {
    override val id: RuleId = RuleId("no-trailing-blank-lines")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        val lastContentLine = content.linesWithRanges().lastOrNull { line -> line.text.isNotBlank() } ?: return
        if (lastContentLine.newlineEndOffset >= content.length) return

        val range = content.rangeAtOffsets(lastContentLine.newlineEndOffset, content.length)
        reporter.report(
            RuleDiagnostic(
                severity = defaultSeverity,
                message = "File should not end with blank lines.",
                file = context.file,
                range = range,
                database = context.database,
                fixes =
                    listOf(
                        Fix(
                            title = "Remove trailing blank lines",
                            safety = FixSafety.Safe,
                            edits = listOf(TextEdit(range = range, replacement = "")),
                        ),
                    ),
            ),
        )
    }
}
