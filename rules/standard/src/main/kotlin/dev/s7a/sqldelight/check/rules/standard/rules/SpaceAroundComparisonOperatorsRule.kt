package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports comparison operators without exactly one space on both sides.
 */
public class SpaceAroundComparisonOperatorsRule : Rule {
    override val id: RuleId = RuleId("space-around-comparison-operators")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        val mappedTypes = content.mappedTypeNames(context.database.dialect.sourcePatterns).toList()
        content.reportOperatorSpacing(
            context = context,
            reporter = reporter,
            rule = this,
            operators =
                content.sqlCharacters()
                    .filterNot { character -> mappedTypes.containsOffset(character.offset) }
                    .mapNotNull { character -> content.comparisonOperatorAt(character.offset) },
            canNormalize = content::canNormalizeInlineSpacing,
            message = { operatorText -> "Comparison operator '$operatorText' should have one space on both sides." },
            fixTitle = "Normalize comparison operator spacing",
        )
    }
}
