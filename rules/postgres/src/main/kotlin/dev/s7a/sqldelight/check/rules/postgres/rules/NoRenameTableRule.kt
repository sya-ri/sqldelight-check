package dev.s7a.sqldelight.check.rules.postgres.rules

import dev.s7a.sqldelight.check.api.DialectCapability
import dev.s7a.sqldelight.check.dialects.postgres.PostgresDialectCapability
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.AlterTableStatementStart
import dev.s7a.sqldelight.check.dialects.postgres.TableRenameOperation
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext
import dev.s7a.sqldelight.check.rule.api.findSourcePatternsInOrder
import dev.s7a.sqldelight.check.rule.api.reportSqlStatementMatches

/**
 * Reports PostgreSQL RENAME TABLE migrations.
 *
 * Renaming tables can break live application versions that still reference the
 * old name.
 */
public class NoRenameTableRule : Rule {
    override val id: RuleId = RuleId("no-rename-table")
    override val defaultSeverity: Severity = Severity.Error
    override val defaultEnable: Boolean = true
    override val targetCapability: DialectCapability = PostgresDialectCapability

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        reportSqlStatementMatches(
            context = context,
            reporter = reporter,
            message = "Avoid renaming PostgreSQL tables in a migration that may run against live application code.",
        ) { statement ->
            statement.findSourcePatternsInOrder(
                context.database.dialect.sourcePatterns,
                AlterTableStatementStart,
                TableRenameOperation,
            )
        }
    }
}
