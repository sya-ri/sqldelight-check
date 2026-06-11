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
 * Reports whitespace before the first SQLDelight declaration or statement.
 */
public class NoLeadingWhitespaceRule : Rule {
    override val id: String = "no-leading-whitespace"
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        val firstContentOffset = content.indexOfFirst { character -> !character.isLeadingWhitespace() }
        val endOffset = if (firstContentOffset == -1) content.length else firstContentOffset
        if (endOffset == 0) return

        val range = content.rangeAtOffsets(0, endOffset)
        reporter.report(
            Diagnostic(
                ruleId = RuleId(id),
                severity = defaultSeverity,
                message = "File should not start with whitespace.",
                file = context.file,
                range = range,
                database = context.database,
                fixes =
                    listOf(
                        Fix(
                            title = "Remove leading whitespace",
                            safety = FixSafety.Safe,
                            edits = listOf(TextEdit(range = range, replacement = "")),
                        ),
                    ),
            ),
        )
    }
}

private fun Char.isLeadingWhitespace(): Boolean =
    this == ' ' || this == '\t' || this == '\n' || this == '\r'
