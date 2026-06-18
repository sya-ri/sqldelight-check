package dev.s7a.sqldelight.check.rules.postgres.refinements

import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.QualifiedRuleId
import dev.s7a.sqldelight.check.rule.api.DiagnosticRefinement
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Suppresses `standard:source-indentation` for PostgreSQL trigger clauses.
 *
 * PostgreSQL trigger definitions commonly place clauses such as `BEFORE`,
 * `FOR EACH ROW`, and `EXECUTE FUNCTION` on their own lines under
 * `CREATE TRIGGER`.
 */
public class PostgresTriggerSourceIndentationRefinement : DiagnosticRefinement {
    override val targetRuleId: QualifiedRuleId = QualifiedRuleId("standard:source-indentation")

    override fun refine(
        context: RuleContext,
        diagnostic: Diagnostic,
    ): Diagnostic? =
        refineDiagnostic(context, diagnostic, String::isTriggerClauseIndentationDiagnostic)
}
