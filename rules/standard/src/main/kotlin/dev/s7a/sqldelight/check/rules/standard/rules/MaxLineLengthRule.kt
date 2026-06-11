package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

import dev.s7a.sqldelight.check.rule.api.booleanOption
import dev.s7a.sqldelight.check.rule.api.commaSeparatedOption
import dev.s7a.sqldelight.check.rule.api.positiveIntOption

import dev.s7a.sqldelight.check.api.RuleDiagnostic
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
    override val id: RuleId = RuleId("max-line-length")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        val maxLineLength = context.options.positiveIntOption("max", DEFAULT_MAX_LINE_LENGTH)
        val ignoreComments = context.options.booleanOption("ignoreComments", false)
        content
            .linesWithRanges()
            .filter { line ->
                line.text.isNotBlank() &&
                    line.text.length > maxLineLength &&
                    !(ignoreComments && line.text.isCommentLine())
            }
            .forEach { line ->
                reporter.report(
                    RuleDiagnostic(
                        severity = defaultSeverity,
                        message = "Line is longer than $maxLineLength characters.",
                        file = context.file,
                        range =
                            content.rangeAtOffsets(
                                line.startOffset + maxLineLength,
                                line.endOffset,
                            ),
                        database = context.database,
                    ),
                )
            }
    }
}

private fun String.isCommentLine(): Boolean {
    val trimmed = trimStart()
    return trimmed.startsWith("--") || trimmed.startsWith("/*") || trimmed.startsWith("*")
}
