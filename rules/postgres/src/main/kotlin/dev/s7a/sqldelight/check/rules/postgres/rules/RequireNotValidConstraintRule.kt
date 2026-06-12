package dev.s7a.sqldelight.check.rules.postgres.rules

import dev.s7a.sqldelight.check.api.DialectCapability
import dev.s7a.sqldelight.check.dialects.postgres.PostgresDialectCapability
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.AlterTableStatementStart
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.ConstraintAddOperation
import dev.s7a.sqldelight.check.dialects.postgres.NotValidConstraintClause
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext
import dev.s7a.sqldelight.check.rule.api.containsSourcePattern
import dev.s7a.sqldelight.check.rule.api.findSourcePatternsInOrder
import dev.s7a.sqldelight.check.rule.api.reportSqlStatementMatches

/**
 * Reports PostgreSQL ADD CONSTRAINT statements that omit NOT VALID.
 *
 * Adding constraints as `NOT VALID` lets validation happen in a separate step
 * with reduced migration risk.
 */
public class RequireNotValidConstraintRule : Rule {
    override val id: RuleId = RuleId("require-not-valid-constraint")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true
    override val targetCapability: DialectCapability = PostgresDialectCapability

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        reportSqlStatementMatches(
            context = context,
            reporter = reporter,
            message = "Add PostgreSQL constraints as NOT VALID and validate them in a later migration.",
        ) { statement ->
            if (statement.containsSourcePattern(NotValidConstraintClause, context.database.dialect.sourcePatterns)) {
                return@reportSqlStatementMatches null
            }
            statement.findSourcePatternsInOrder(
                context.database.dialect.sourcePatterns,
                AlterTableStatementStart,
                ConstraintAddOperation,
            )
        }
    }
}
