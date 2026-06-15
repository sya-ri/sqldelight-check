package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SqlDialectSourceTerm
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports DELETE statements guarded by an obvious always-true predicate.
 */
public class NoDeleteAllRule : Rule {
    override val id: RuleId = RuleId("no-delete-all")
    override val defaultSeverity: Severity = Severity.Error
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        reportAlwaysTrueWhere(
            reporter = reporter,
            context = context,
            statementTerm = SqlDialectSourceTerm.Delete,
            message = "DELETE statements should not use an always-true WHERE predicate.",
        )
    }
}
