package dev.s7a.sqldelight.check.rules.hsql.rules

import dev.s7a.sqldelight.check.api.DialectId
import dev.s7a.sqldelight.check.dialects.hsql.HsqlDialectId
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.dialects.hsql.DatabaseFileSettingStatement
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext
import dev.s7a.sqldelight.check.rule.api.findSourcePattern
import dev.s7a.sqldelight.check.rule.api.reportSqlStatementMatches

/**
 * Reports HSQL database or file settings in SQLDelight schema and migration sources.
 *
 * These settings change database-wide operational behavior and are safer to keep
 * in database bootstrap or administration code instead of versioned DDL files.
 */
public class NoDatabaseFileSettingsRule : Rule {
    override val id: RuleId = RuleId("no-database-file-settings")
    override val defaultSeverity: Severity = Severity.Error
    override val defaultEnable: Boolean = true
    override val targetDialect: DialectId = HsqlDialectId

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        reportSqlStatementMatches(
            context = context,
            reporter = reporter,
            message = "Keep HSQL database and file settings out of SQLDelight schema migrations.",
        ) { statement ->
            statement.findSourcePattern(DatabaseFileSettingStatement, context.database.dialect.sourcePatterns)
        }
    }
}
