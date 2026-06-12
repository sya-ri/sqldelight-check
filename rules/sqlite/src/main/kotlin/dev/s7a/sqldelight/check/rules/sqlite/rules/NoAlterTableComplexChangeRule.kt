package dev.s7a.sqldelight.check.rules.sqlite.rules

import dev.s7a.sqldelight.check.api.DialectCapability
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.AlterTableStatementStart
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.ComplexAlterTableOperation
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext
import dev.s7a.sqldelight.check.rule.api.findSourcePattern
import dev.s7a.sqldelight.check.rule.api.reportSqlStatementMatches

/**
 * Reports SQLite ALTER TABLE operations that require an explicit table rebuild strategy.
 *
 * The rule flags migration forms that SQLite cannot apply as simple in-place
 * table alterations.
 */
public class NoAlterTableComplexChangeRule : Rule {
    override val id: RuleId = RuleId("no-alter-table-complex-change")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true
    override val targetCapability: DialectCapability = DialectCapability.SQLite

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        reportSqlStatementMatches(
            context = context,
            reporter = reporter,
            message = "Avoid complex SQLite ALTER TABLE changes; rebuild the table explicitly.",
        ) { statement ->
            if (statement.findSourcePattern(AlterTableStatementStart, context.database.dialect.sourcePatterns) == null) {
                return@reportSqlStatementMatches null
            }
            statement.findSourcePattern(ComplexAlterTableOperation, context.database.dialect.sourcePatterns)
        }
    }
}
