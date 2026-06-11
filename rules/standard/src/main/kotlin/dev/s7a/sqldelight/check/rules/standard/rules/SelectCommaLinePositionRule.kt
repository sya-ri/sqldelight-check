package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports multiline SELECT list commas that are not trailing the previous target.
 */
public class SelectCommaLinePositionRule : Rule {
    override val id: RuleId = RuleId("select-comma-line-position")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        val lines = content.linesWithRanges()
        content.sourceSelectClauseTargets().forEach { clause ->
            if (clause.targets.size < 2) return@forEach
            if (!content.selectTargetListText(clause).contains('\n')) return@forEach

            clause.targets.forEach { target ->
                val commaOffset = target.commaOffset ?: return@forEach
                val targetLine = lines.lineContaining(target.endOffset - 1) ?: return@forEach
                val commaLine = lines.lineContaining(commaOffset) ?: return@forEach
                if (targetLine.number == commaLine.number) return@forEach

                reporter.report(
                    RuleDiagnostic(
                        severity = defaultSeverity,
                        message = "Multiline SELECT list commas should trail the previous result expression.",
                        file = context.file,
                        range = content.rangeAtOffsets(commaOffset, commaOffset + 1),
                        database = context.database,
                    ),
                )
            }
        }
    }
}

private fun String.selectTargetListText(clause: SourceSelectClauseTargets): String =
    substring(clause.targets.first().startOffset, clause.targets.last().endOffset)
