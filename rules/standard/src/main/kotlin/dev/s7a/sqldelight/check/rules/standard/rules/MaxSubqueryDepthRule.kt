package dev.s7a.sqldelight.check.rules.standard.rules

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

private const val DEFAULT_MAX_SUBQUERY_DEPTH = 3

/**
 * Reports SELECT statements nested deeper than the configured parenthesis depth.
 */
public class MaxSubqueryDepthRule : Rule {
    override val id: RuleId = RuleId("max-subquery-depth")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val maxDepth = context.options.positiveIntOption("maxDepth", DEFAULT_MAX_SUBQUERY_DEPTH)
        val content = context.file.content
        content.sqlTokens()
            .filter { token -> token.isKeyword("select") }
            .forEach { token ->
                val depth = content.sqlParenthesisDepthAt(token.startOffset)
                if (depth <= maxDepth) return@forEach
                reporter.report(
                    RuleDiagnostic(
                        severity = defaultSeverity,
                        message = "Subquery nesting depth is greater than $maxDepth.",
                        file = context.file,
                        range = content.rangeAtOffsets(token.startOffset, token.endOffset),
                        database = context.database,
                    ),
                )
            }
    }
}
