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
 * Reports line comments whose `--` marker is not followed by a space.
 */
public class SpaceAfterLineCommentMarkerRule : Rule {
    override val id: RuleId = RuleId("space-after-line-comment-marker")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        content.lineComments().forEach { comment ->
            val markerEnd = comment.startOffset + 2
            if (markerEnd >= content.length) return@forEach
            val next = content[markerEnd]
            if (next == ' ' || next == '\r' || next == '\n') return@forEach

            val range = content.rangeAtOffsets(markerEnd, markerEnd)
            reporter.report(
                RuleDiagnostic(
                    severity = defaultSeverity,
                    message = "Line comment marker should be followed by one space.",
                    file = context.file,
                    range = range,
                    database = context.database,
                    fixes =
                        listOf(
                            Fix(
                                title = "Insert space after line comment marker",
                                safety = FixSafety.Safe,
                                edits = listOf(TextEdit(range = range, replacement = " ")),
                            ),
                        ),
                ),
            )
        }
    }
}
