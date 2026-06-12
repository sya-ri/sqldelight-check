package dev.s7a.sqldelight.check.rules.postgres.rules

import dev.s7a.sqldelight.check.api.DialectId
import dev.s7a.sqldelight.check.dialects.postgres.PostgresDialectId
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.dialects.postgres.SerialDataTypeName
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext
import dev.s7a.sqldelight.check.rule.api.findSourcePattern
import dev.s7a.sqldelight.check.rule.api.reportSqlStatementMatches

/**
 * Reports PostgreSQL serial pseudo-types when identity columns are preferred.
 *
 * Identity columns are the modern PostgreSQL mechanism for generated numeric
 * keys.
 */
public class PreferIdentityOverSerialRule : Rule {
    override val id: RuleId = RuleId("prefer-identity-over-serial")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true
    override val targetDialect: DialectId = PostgresDialectId

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        reportSqlStatementMatches(
            context = context,
            reporter = reporter,
            message = "Prefer GENERATED AS IDENTITY columns over PostgreSQL serial types.",
        ) { statement ->
            statement.findSourcePattern(SerialDataTypeName, context.database.dialect.sourcePatterns)
        }
    }
}
