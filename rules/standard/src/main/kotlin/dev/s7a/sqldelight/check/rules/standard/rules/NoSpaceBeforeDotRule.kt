package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports spaces or tabs before dot tokens in qualified names.
 */
public class NoSpaceBeforeDotRule : Rule {
    override val id: RuleId = RuleId("standard:no-space-before-dot")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnablement: Enablement = Enablement.Auto

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        context.file.content.reportNoSpaceBeforeToken(
            context = context,
            reporter = reporter,
            rule = this,
            token = '.',
            message = "Dot should not be preceded by whitespace.",
            fixTitle = "Remove whitespace before dot",
        )
    }
}
