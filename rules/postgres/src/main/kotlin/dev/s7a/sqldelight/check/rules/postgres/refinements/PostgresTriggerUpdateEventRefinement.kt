package dev.s7a.sqldelight.check.rules.postgres.refinements

import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.QualifiedRuleId
import dev.s7a.sqldelight.check.rule.api.DiagnosticRefinement
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Suppresses `standard:no-update-without-where` for PostgreSQL trigger events.
 *
 * PostgreSQL trigger definitions can contain `BEFORE UPDATE`, `AFTER UPDATE`,
 * or `INSTEAD OF UPDATE` clauses. Those clauses describe trigger events, not
 * executable `UPDATE` statements.
 */
public class PostgresTriggerUpdateEventRefinement : DiagnosticRefinement {
    override val targetRuleId: QualifiedRuleId = QualifiedRuleId("standard:no-update-without-where")

    override fun refine(
        context: RuleContext,
        diagnostic: Diagnostic,
    ): Diagnostic? =
        refineDiagnostic(context, diagnostic, String::isTriggerUpdateEvent)
}
