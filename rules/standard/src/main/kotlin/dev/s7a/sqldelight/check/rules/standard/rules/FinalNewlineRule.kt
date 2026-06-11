package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.Fix
import dev.s7a.sqldelight.check.api.FixSafety
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SourcePosition
import dev.s7a.sqldelight.check.api.SourceRange
import dev.s7a.sqldelight.check.api.TextEdit
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports files that do not end with a newline.
 */
public class FinalNewlineRule : Rule {
    override val id: RuleId = RuleId("final-newline")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        if (content.isEmpty() || content.endsWith('\n')) return

        val position = content.endPosition()
        val range = SourceRange(start = position, end = position)
        reporter.report(
            RuleDiagnostic(
                severity = defaultSeverity,
                message = "File should end with a newline.",
                file = context.file,
                range = range,
                database = context.database,
                fixes =
                    listOf(
                        Fix(
                            title = "Insert final newline",
                            safety = FixSafety.Safe,
                            edits = listOf(TextEdit(range = range, replacement = "\n")),
                        ),
                    ),
            ),
        )
    }
}

private fun String.endPosition(): SourcePosition {
    val lastNewline = lastIndexOf('\n')
    val line = count { character -> character == '\n' } + 1
    val column = if (lastNewline == -1) length + 1 else length - lastNewline
    return SourcePosition(line = line, column = column)
}
