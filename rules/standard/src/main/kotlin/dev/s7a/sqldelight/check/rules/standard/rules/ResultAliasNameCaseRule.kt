package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext
import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

/**
 * Reports result column aliases that are not snake case.
 */
public class ResultAliasNameCaseRule : Rule {
    override val id: RuleId = RuleId("result-alias-name-case")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        content.resultColumnAliases().forEach { alias ->
            if (alias.token.text.isSnakeCaseIdentifier()) return@forEach
            reporter.report(
                RuleDiagnostic(
                    severity = defaultSeverity,
                    message = "Result column alias '${alias.token.text}' should be snake case.",
                    file = context.file,
                    range = content.rangeAtOffsets(alias.token.startOffset, alias.token.endOffset),
                    database = context.database,
                ),
            )
        }
    }
}

internal fun String.isSnakeCaseIdentifier(): Boolean =
    isNotEmpty() &&
        first().isLowerCase() &&
        all { character -> character.isLowerCase() || character.isDigit() || character == '_' } &&
        any { character -> character.isLetter() } &&
        !contains("__") &&
        !endsWith("_")
