package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports multiline SELECT lists that do not put every target on its own line.
 */
public class SelectTargetNewlineRule : Rule {
    override val id: RuleId = RuleId("standard:select-target-newline")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnablement: Enablement = Enablement.Auto

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        val lines = content.linesWithRanges()
        content.sourceSelectClauseTargets().forEach { clause ->
            if (clause.targets.size < 2) return@forEach
            if (!content.selectTargetListText(clause).contains('\n')) return@forEach
            if (clause.targets.all { target -> lines.lineContaining(target.startOffset)?.firstNonWhitespaceOffset == target.startOffset }) {
                return@forEach
            }

            reporter.report(
                Diagnostic(
                    ruleId = id,
                    severity = defaultSeverity,
                    message = "Multiline SELECT lists should put each result expression on its own line.",
                    file = context.file,
                    range = content.rangeAtOffsets(clause.select.startOffset, clause.listEndOffset),
                    database = context.database,
                ),
            )
        }
    }
}

private fun String.selectTargetListText(clause: SourceSelectClauseTargets): String =
    substring(clause.targets.first().startOffset, clause.targets.last().endOffset)
