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
 * Reports spaces or tabs after dot tokens in qualified names.
 */
public class NoSpaceAfterDotRule : Rule {
    override val id: RuleId = RuleId("no-space-after-dot")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        content
            .sqlCharacters()
            .filter { character -> character.value == '.' }
            .forEach { dot ->
                val whitespaceEnd = content.horizontalWhitespaceEndAfter(dot.offset + 1)
                if (whitespaceEnd == dot.offset + 1) return@forEach

                val range = content.rangeAtOffsets(dot.offset + 1, whitespaceEnd)
                reporter.report(
                    RuleDiagnostic(
                        severity = defaultSeverity,
                        message = "Dot should not be followed by whitespace.",
                        file = context.file,
                        range = range,
                        database = context.database,
                        fixes =
                            listOf(
                                Fix(
                                    title = "Remove whitespace after dot",
                                    safety = FixSafety.Safe,
                                    edits = listOf(TextEdit(range = range, replacement = "")),
                                ),
                            ),
                    ),
                )
            }
    }
}
