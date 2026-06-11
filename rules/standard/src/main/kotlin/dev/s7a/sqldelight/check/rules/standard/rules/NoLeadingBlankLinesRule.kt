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
 * Reports blank lines before the first SQLDelight declaration or statement.
 */
public class NoLeadingBlankLinesRule : Rule {
    override val id: RuleId = RuleId("no-leading-blank-lines")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        val firstContentLine = content.linesWithRanges().firstOrNull { line -> line.text.isNotBlank() } ?: return
        if (firstContentLine.startOffset == 0) return

        val range = content.rangeAtOffsets(0, firstContentLine.startOffset)
        reporter.report(
            Diagnostic(
                ruleId = id,
                severity = defaultSeverity,
                message = "File should not start with blank lines.",
                file = context.file,
                range = range,
                database = context.database,
                fixes =
                    listOf(
                        Fix(
                            title = "Remove leading blank lines",
                            safety = FixSafety.Safe,
                            edits = listOf(TextEdit(range = range, replacement = "")),
                        ),
                    ),
            ),
        )
    }
}
