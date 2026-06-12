package dev.s7a.sqldelight.check.rules.postgres.rules

import dev.s7a.sqldelight.check.api.DialectCapability
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.AlterTableStatementStart
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.ColumnAddOperation
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.DefaultValueClause
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.VolatileDefaultFunction
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext
import dev.s7a.sqldelight.check.rule.api.findSourcePatternsInOrder
import dev.s7a.sqldelight.check.rule.api.reportSqlStatementMatches

/**
 * Reports PostgreSQL ADD COLUMN statements that use volatile defaults.
 *
 * Volatile defaults can rewrite or evaluate many existing rows during the
 * migration.
 */
public class NoAddColumnWithVolatileDefaultRule : Rule {
    override val id: RuleId = RuleId("no-add-column-with-volatile-default")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true
    override val targetCapability: DialectCapability = DialectCapability.PostgreSql

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        reportSqlStatementMatches(
            context = context,
            reporter = reporter,
            message = "Avoid adding a column with a volatile default in a PostgreSQL migration.",
        ) { statement ->
            statement.findSourcePatternsInOrder(
                context.database.dialect.sourcePatterns,
                AlterTableStatementStart,
                ColumnAddOperation,
                DefaultValueClause,
                VolatileDefaultFunction,
            )
        }
    }
}
