package dev.s7a.sqldelight.check.rules.hsql.rules

import dev.s7a.sqldelight.check.api.DialectCapability
import dev.s7a.sqldelight.check.dialects.hsql.HsqlDialectCapability
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.dialects.hsql.SystemOperationStatement
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext
import dev.s7a.sqldelight.check.rule.api.findSourcePattern
import dev.s7a.sqldelight.check.rule.api.reportSqlStatementMatches

/**
 * Reports HSQL system operations in SQLDelight schema and migration sources.
 *
 * Operations such as CHECKPOINT, SHUTDOWN, BACKUP, SCRIPT, and bulk import or
 * export are administrative actions rather than schema changes.
 */
public class NoSystemOperationsRule : Rule {
    override val id: RuleId = RuleId("no-system-operations")
    override val defaultSeverity: Severity = Severity.Error
    override val defaultEnable: Boolean = true
    override val targetCapability: DialectCapability = HsqlDialectCapability

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        reportSqlStatementMatches(
            context = context,
            reporter = reporter,
            message = "Keep HSQL system operations out of SQLDelight schema migrations.",
        ) { statement ->
            statement.findSourcePattern(SystemOperationStatement, context.database.dialect.sourcePatterns)
        }
    }
}
