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
 * Reports common binary arithmetic and concatenation operators without one space on both sides.
 */
public class SpaceAroundBinaryOperatorsRule : Rule {
    override val id: RuleId = RuleId("space-around-binary-operators")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        content
            .sqlCharacters()
            .mapNotNull { character -> content.binaryOperatorAt(character.offset) }
            .distinctBy { operator -> operator.startOffset }
            .forEach { operator ->
                val leftStart = content.horizontalWhitespaceStartBefore(operator.startOffset)
                val rightEnd = content.horizontalWhitespaceEndAfter(operator.endOffset)
                if (!content.canNormalizeBinaryOperatorSpacing(leftStart, operator.startOffset, operator.endOffset, rightEnd)) {
                    return@forEach
                }

                val operatorText = content.substring(operator.startOffset, operator.endOffset)
                val currentText = content.substring(leftStart, rightEnd)
                val replacement = " $operatorText "
                if (currentText == replacement) return@forEach

                val range = content.rangeAtOffsets(leftStart, rightEnd)
                reporter.report(
                    RuleDiagnostic(
                        severity = defaultSeverity,
                        message = "Binary operator '$operatorText' should have one space on both sides.",
                        file = context.file,
                        range = range,
                        database = context.database,
                        fixes =
                            listOf(
                                Fix(
                                    title = "Normalize binary operator spacing",
                                    safety = FixSafety.Unsafe,
                                    edits = listOf(TextEdit(range = range, replacement = replacement)),
                                ),
                            ),
                    ),
                )
            }
    }
}
