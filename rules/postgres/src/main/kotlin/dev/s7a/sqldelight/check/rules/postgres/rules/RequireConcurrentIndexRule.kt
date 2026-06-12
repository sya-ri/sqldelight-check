package dev.s7a.sqldelight.check.rules.postgres.rules

import dev.s7a.sqldelight.check.api.DialectId
import dev.s7a.sqldelight.check.dialects.postgres.PostgresDialectId
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.dialects.postgres.CreateConcurrentIndexStatementStart
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.CreateIndexStatementStart
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext
import dev.s7a.sqldelight.check.rule.api.containsSourcePattern
import dev.s7a.sqldelight.check.rule.api.findSourcePattern
import dev.s7a.sqldelight.check.rule.api.reportSqlStatementMatches

/**
 * Reports PostgreSQL index creation statements that omit CONCURRENTLY.
 *
 * Concurrent index builds reduce write blocking for indexes created on live
 * tables.
 */
public class RequireConcurrentIndexRule : Rule {
    override val id: RuleId = RuleId("require-concurrent-index")
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
            if (statement.containsSourcePattern(CreateConcurrentIndexStatementStart, context.database.dialect.sourcePatterns)) {
                return@reportSqlStatementMatches null
            }
            statement.findSourcePattern(CreateIndexStatementStart, context.database.dialect.sourcePatterns)
        }
    }
}
