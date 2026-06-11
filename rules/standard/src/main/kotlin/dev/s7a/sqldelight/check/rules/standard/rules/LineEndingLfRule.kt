package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.RuleDiagnostic
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
 * Reports CRLF and CR line endings.
 */
public class LineEndingLfRule : Rule {
    override val id: RuleId = RuleId("line-ending-lf")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        var index = 0
        while (index < content.length) {
            if (content[index] != '\r') {
                index++
                continue
            }

            val endOffset = if (index + 1 < content.length && content[index + 1] == '\n') index + 2 else index + 1
            val range = content.rangeAtOffsets(index, endOffset)
            reporter.report(
                RuleDiagnostic(
                    severity = defaultSeverity,
                    message = "Line ending should be LF.",
                    file = context.file,
                    range = range,
                    database = context.database,
                    fixes =
                        listOf(
                            Fix(
                                title = "Replace line ending with LF",
                                safety = FixSafety.Safe,
                                edits = listOf(TextEdit(range = range, replacement = "\n")),
                            ),
                        ),
                ),
            )
            index = endOffset
        }
    }
}
