package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext
import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

/**
 * Reports SELECT * in CREATE VIEW definitions.
 */
public class NoSelectStarInViewRule : Rule {
    override val id: RuleId = RuleId("no-select-star-in-view")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        val viewStarts = content.viewNameTokens().map { token -> token.startOffset }.toList()
        if (viewStarts.isEmpty()) return

        content.sourceSelectClauseTargets()
            .filter { clause -> viewStarts.any { viewStart -> viewStart < clause.select.startOffset && content.statementEndAfter(viewStart) >= clause.select.startOffset } }
            .flatMap { clause -> clause.targets }
            .filter { target -> content.substring(target.startOffset, target.endOffset).trim() == "*" }
            .forEach { target ->
                reporter.report(
                    RuleDiagnostic(
                        severity = defaultSeverity,
                        message = "Views should declare explicit result columns instead of SELECT *.",
                        file = context.file,
                        range = content.rangeAtOffsets(target.startOffset, target.endOffset),
                        database = context.database,
                    ),
                )
            }
    }
}
