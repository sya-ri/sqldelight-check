package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

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
 * Reports mixed `!=` and `<>` not-equal operators in the same file.
 */
public class ConsistentNotEqualOperatorRule : Rule {
    override val id: RuleId = RuleId("consistent-not-equal-operator")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        var preferredOperator: String? = null
        content
            .sqlCharacters()
            .mapNotNull { character -> content.comparisonOperatorAt(character.offset) }
            .distinctBy { operator -> operator.startOffset }
            .forEach { operator ->
                val operatorText = content.substring(operator.startOffset, operator.endOffset)
                if (operatorText !in setOf("!=", "<>")) return@forEach

                val preferred = preferredOperator
                if (preferred == null) {
                    preferredOperator = operatorText
                    return@forEach
                }
                if (operatorText == preferred) return@forEach

                val range = content.rangeAtOffsets(operator.startOffset, operator.endOffset)
                reporter.report(
                    RuleDiagnostic(
                        severity = defaultSeverity,
                        message = "Use a consistent not-equal operator; expected '$preferred'.",
                        file = context.file,
                        range = range,
                        database = context.database,
                        fixes =
                            listOf(
                                Fix(
                                    title = "Replace not-equal operator with $preferred",
                                    safety = FixSafety.Unsafe,
                                    edits = listOf(TextEdit(range = range, replacement = preferred)),
                                ),
                            ),
                    ),
                )
            }
    }
}
