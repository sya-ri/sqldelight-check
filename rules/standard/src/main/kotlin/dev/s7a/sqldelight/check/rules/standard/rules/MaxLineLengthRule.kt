package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

private const val DEFAULT_MAX_LINE_LENGTH = 120

/**
 * Reports non-blank lines that exceed the maximum line length.
 */
public class MaxLineLengthRule : Rule {
    override val id: RuleId = RuleId("standard:max-line-length")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnablement: Enablement = Enablement.Auto

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        content
            .linesWithRanges()
            .filter { line -> line.text.isNotBlank() && line.text.length > DEFAULT_MAX_LINE_LENGTH }
            .forEach { line ->
                reporter.report(
                    Diagnostic(
                        ruleId = id,
                        severity = defaultSeverity,
                        message = "Line is longer than $DEFAULT_MAX_LINE_LENGTH characters.",
                        file = context.file,
                        range =
                            content.rangeAtOffsets(
                                line.startOffset + DEFAULT_MAX_LINE_LENGTH,
                                line.endOffset,
                            ),
                        database = context.database,
                    ),
                )
            }
    }
}
