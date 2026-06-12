package dev.s7a.sqldelight.check.rules.postgres.rules

import dev.s7a.sqldelight.check.api.DialectCapability
import dev.s7a.sqldelight.check.dialects.postgres.PostgresDialectCapability
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.AlterTableStatementStart
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.ColumnAlterOperation
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.ColumnSetNotNullOperation
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext
import dev.s7a.sqldelight.check.rule.api.findSourcePatternsInOrder
import dev.s7a.sqldelight.check.rule.api.reportSqlStatementMatches

/**
 * Reports PostgreSQL ALTER COLUMN SET NOT NULL migrations on existing columns.
 *
 * Existing-column nullability changes should use a separate validation strategy
 * before enforcing the constraint.
 */
public class NoSetNotNullOnExistingColumnRule : Rule {
    override val id: RuleId = RuleId("no-set-not-null-on-existing-column")
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
            message = "Avoid SET NOT NULL on existing PostgreSQL columns without a separate validation strategy.",
        ) { statement ->
            statement.findSourcePatternsInOrder(
                context.database.dialect.sourcePatterns,
                AlterTableStatementStart,
                ColumnAlterOperation,
                ColumnSetNotNullOperation,
            )
        }
    }
}
