package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports spaces or tabs before closing parenthesis tokens.
 */
public class NoSpaceBeforeClosingParenthesisRule : Rule {
    override val id: RuleId = RuleId("no-space-before-closing-parenthesis")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        context.file.content.reportNoSpaceBeforeToken(
            context = context,
            reporter = reporter,
            rule = this,
            token = ')',
            message = "Closing parenthesis should not be preceded by whitespace.",
            fixTitle = "Remove whitespace before closing parenthesis",
        )
    }
}
