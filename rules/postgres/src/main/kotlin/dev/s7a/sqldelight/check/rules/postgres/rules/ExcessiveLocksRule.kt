package dev.s7a.sqldelight.check.rules.postgres.rules

import dev.s7a.sqldelight.check.api.DialectId
import dev.s7a.sqldelight.check.dialects.postgres.PostgresDialectId
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext
import dev.s7a.sqldelight.check.rule.api.reportSqlStatementMatches

/**
 * Reports PostgreSQL DDL patterns that can take strong locks.
 *
 * The rule highlights migration statements that commonly need an online
 * migration strategy.
 */
public class ExcessiveLocksRule : Rule {
    override val id: RuleId = RuleId("excessive-locks")
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
            message = "Use CREATE INDEX CONCURRENTLY for PostgreSQL indexes that may be built on live tables.",
        ) { statement ->
            statement.findCreateIndexRequiringConcurrentBuild(context)
        }
    }
}
