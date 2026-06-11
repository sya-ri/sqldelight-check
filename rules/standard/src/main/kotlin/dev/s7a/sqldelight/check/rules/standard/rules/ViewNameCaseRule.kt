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
 * Reports CREATE VIEW names that are not snake case.
 */
public class ViewNameCaseRule : Rule {
    override val id: RuleId = RuleId("view-name-case")
    override val defaultSeverity: Severity = Severity.Info
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        content.viewNameTokens().forEach { token ->
            if (token.text.isSnakeCaseIdentifier()) return@forEach
            reporter.report(
                RuleDiagnostic(
                    severity = defaultSeverity,
                    message = "View name '${token.text}' should be snake case.",
                    file = context.file,
                    range = content.rangeAtOffsets(token.startOffset, token.endOffset),
                    database = context.database,
                ),
            )
        }
    }
}

internal fun String.viewNameTokens(): Sequence<SqlToken> =
    sequence {
        val tokens = sqlTokens().toList()
        tokens.windowed(size = 3).forEach { (create, view, name) ->
            if (create.isKeyword("create") && view.isKeyword("view")) {
                yield(name)
            }
        }
    }
