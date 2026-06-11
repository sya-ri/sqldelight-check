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
 * Reports tab characters in leading indentation.
 */
public class NoTabIndentationRule : Rule {
    override val id: RuleId = RuleId("no-tab-indentation")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        content.linesWithRanges().forEach { line ->
            val indentation = line.text.takeWhile { character -> character == ' ' || character == '\t' }
            if ('\t' !in indentation) return@forEach

            val replacement = indentation.replace("\t", "    ")
            val range = content.rangeAtOffsets(line.startOffset, line.startOffset + indentation.length)
            reporter.report(
                RuleDiagnostic(
                    severity = defaultSeverity,
                    message = "Indentation should use spaces instead of tabs.",
                    file = context.file,
                    range = range,
                    database = context.database,
                    fixes =
                        listOf(
                            Fix(
                                title = "Replace indentation tabs with spaces",
                                safety = FixSafety.Safe,
                                edits = listOf(TextEdit(range = range, replacement = replacement)),
                            ),
                        ),
                ),
            )
        }
    }
}
