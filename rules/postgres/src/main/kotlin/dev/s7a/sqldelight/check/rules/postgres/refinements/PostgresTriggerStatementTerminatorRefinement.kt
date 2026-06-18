@file:Suppress("DuplicatedCode")

package dev.s7a.sqldelight.check.rules.postgres.refinements

import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.QualifiedRuleId
import dev.s7a.sqldelight.check.rule.api.DiagnosticRefinement
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Suppresses `standard:statement-terminator` at PostgreSQL trigger body boundaries.
 *
 * PostgreSQL trigger definitions can continue from the trigger header into
 * `EXECUTE FUNCTION`, `EXECUTE PROCEDURE`, or trigger body syntax without a
 * semicolon at intermediate clause boundaries.
 */
public class PostgresTriggerStatementTerminatorRefinement : DiagnosticRefinement {
    override val targetRuleId: QualifiedRuleId = QualifiedRuleId("standard:statement-terminator")

    override fun refine(
        context: RuleContext,
        diagnostic: Diagnostic,
    ): Diagnostic? =
        refineDiagnostic(context, diagnostic, String::isTriggerBodyBoundaryTerminatorDiagnostic)
}
