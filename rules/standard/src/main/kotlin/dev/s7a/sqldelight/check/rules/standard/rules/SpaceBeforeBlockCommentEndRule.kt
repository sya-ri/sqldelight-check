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
 * Reports block comments whose closing marker is not preceded by a space.
 */
public class SpaceBeforeBlockCommentEndRule : Rule {
    override val id: String = "space-before-block-comment-end"
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        content.blockComments().forEach { comment ->
            if (comment.endOffset - 2 < comment.startOffset || !content.startsWith("*/", comment.endOffset - 2)) {
                return@forEach
            }

            val markerStart = comment.endOffset - 2
            if (markerStart <= comment.startOffset + 2) return@forEach
            val previous = content[markerStart - 1]
            if (previous == ' ' || previous == '\r' || previous == '\n' || previous == '*') return@forEach

            val range = content.rangeAtOffsets(markerStart, markerStart)
            reporter.report(
                Diagnostic(
                    ruleId = RuleId(id),
                    severity = defaultSeverity,
                    message = "Block comment end marker should be preceded by one space.",
                    file = context.file,
                    range = range,
                    database = context.database,
                    fixes =
                        listOf(
                            Fix(
                                title = "Insert space before block comment end",
                                safety = FixSafety.Safe,
                                edits = listOf(TextEdit(range = range, replacement = " ")),
                            ),
                        ),
                ),
            )
        }
    }
}
