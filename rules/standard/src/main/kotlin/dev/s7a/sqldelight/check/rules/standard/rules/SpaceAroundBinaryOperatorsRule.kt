package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
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
        content.reportOperatorSpacing(
            context = context,
            reporter = reporter,
            rule = this,
            operators = content.sqlCharacters().mapNotNull { character -> content.binaryOperatorAt(character.offset) },
            canNormalize = content::canNormalizeBinaryOperatorSpacing,
            message = { operatorText -> "Binary operator '$operatorText' should have one space on both sides." },
            fixTitle = "Normalize binary operator spacing",
        )
    }
}
