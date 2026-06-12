package dev.s7a.sqldelight.check.rules.postgres.rules

import dev.s7a.sqldelight.check.api.DialectCapability
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.ConcurrentlyClause
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.ReindexStatementStart
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.ReindexSystemTarget
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext
import dev.s7a.sqldelight.check.rule.api.containsSourcePattern
import dev.s7a.sqldelight.check.rule.api.findSourcePattern
import dev.s7a.sqldelight.check.rule.api.reportSqlStatementMatches

/**
 * Reports PostgreSQL REINDEX statements that omit CONCURRENTLY.
 *
 * Concurrent reindexing avoids blocking writes for indexes that can be rebuilt
 * online.
 */
public class ReindexConcurrentlyRule : Rule {
    override val id: RuleId = RuleId("reindex-concurrently")
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
            message = "Use REINDEX CONCURRENTLY for PostgreSQL reindex operations on live objects.",
        ) { statement ->
            if (statement.containsSourcePattern(ReindexSystemTarget, context.database.dialect.sourcePatterns)) {
                return@reportSqlStatementMatches null
            }
            if (statement.containsSourcePattern(ConcurrentlyClause, context.database.dialect.sourcePatterns)) {
                return@reportSqlStatementMatches null
            }
            statement.findSourcePattern(ReindexStatementStart, context.database.dialect.sourcePatterns)
        }
    }
}
