package dev.s7a.sqldelight.check.rules.postgres.rules

import dev.s7a.sqldelight.check.api.DialectId
import dev.s7a.sqldelight.check.api.QualifiedRuleId
import dev.s7a.sqldelight.check.dialects.postgres.PostgresDialectId
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.RuleSetId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.AlterTableStatementStart
import dev.s7a.sqldelight.check.dialects.postgres.ColumnDropOperation
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext
import dev.s7a.sqldelight.check.rule.api.RuleDeprecation
import dev.s7a.sqldelight.check.rule.api.findSourcePatternsInOrder
import dev.s7a.sqldelight.check.rule.api.reportSqlStatementMatches

/**
 * Reports PostgreSQL DROP COLUMN migrations.
 *
 * Dropping columns can break live application versions that still read or write
 * the old schema.
 */
public class NoDropColumnRule : Rule {
    override val id: RuleId = RuleId("no-drop-column")
    override val defaultSeverity: Severity = Severity.Error
    override val defaultEnable: Boolean = true
    override val targetDialect: DialectId = PostgresDialectId
    override val deprecation: RuleDeprecation =
        RuleDeprecation(
            message = "This PostgreSQL-specific destructive migration rule has moved to the standard rule set.",
            replacement = QualifiedRuleId(RuleSetId("standard"), RuleId("no-drop-column-in-migration")),
        )

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        reportSqlStatementMatches(
            context = context,
            reporter = reporter,
            message = "Avoid dropping PostgreSQL columns in a migration that may run against live application code.",
        ) { statement ->
            statement.findSourcePatternsInOrder(
                context.database.dialect.sourcePatterns,
                AlterTableStatementStart,
                ColumnDropOperation,
            )
        }
    }
}
