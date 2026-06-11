package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.Enablement
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
 * Reports trailing spaces and tabs.
 */
public class NoTrailingWhitespaceRule : Rule {
    override val id: RuleId = RuleId("no-trailing-whitespace")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        context.file.content
            .lineSequence()
            .forEachIndexed { index, line ->
                val trailingStart = line.indexOfLast { character -> character != ' ' && character != '\t' } + 1
                if (trailingStart == line.length) return@forEachIndexed
                val range =
                    SourceRange(
                        start = SourcePosition(line = index + 1, column = trailingStart + 1),
                        end = SourcePosition(line = index + 1, column = line.length + 1),
                    )
                reporter.report(
                    RuleDiagnostic(
                        severity = defaultSeverity,
                        message = "Line contains trailing whitespace.",
                        file = context.file,
                        range = range,
                        database = context.database,
                        fixes =
                            listOf(
                                Fix(
                                    title = "Remove trailing whitespace",
                                    safety = FixSafety.Safe,
                                    edits = listOf(TextEdit(range = range, replacement = "")),
                                ),
                            ),
                    ),
                )
            }
    }
}
