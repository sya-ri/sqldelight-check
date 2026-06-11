package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.rule.api.booleanOption
import dev.s7a.sqldelight.check.rule.api.commaSeparatedOption
import dev.s7a.sqldelight.check.rule.api.positiveIntOption

import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

private const val DEFAULT_MAX_CASE_DEPTH = 2

/**
 * Reports CASE expressions nested deeper than the configured limit.
 */
public class MaxCaseDepthRule : Rule {
    override val id: RuleId = RuleId("max-case-depth")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val maxDepth = context.options.positiveIntOption("maxDepth", DEFAULT_MAX_CASE_DEPTH)
        val content = context.file.content
        var depth = 0
        content.sqlTokens().forEach { token ->
            when {
                token.isKeyword("case") -> {
                    depth++
                    if (depth > maxDepth) {
                        reporter.report(
                            Diagnostic(
                                ruleId = id,
                                severity = defaultSeverity,
                                message = "CASE nesting depth is greater than $maxDepth.",
                                file = context.file,
                                range = content.rangeAtOffsets(token.startOffset, token.endOffset),
                                database = context.database,
                            ),
                        )
                    }
                }
                token.isKeyword("end") && depth > 0 -> depth--
            }
        }
    }
}
