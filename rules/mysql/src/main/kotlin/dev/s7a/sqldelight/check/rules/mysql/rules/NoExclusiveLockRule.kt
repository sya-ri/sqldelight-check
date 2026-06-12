package dev.s7a.sqldelight.check.rules.mysql.rules

import dev.s7a.sqldelight.check.api.DialectId
import dev.s7a.sqldelight.check.dialects.mysql.MySqlDialectId
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.dialects.mysql.ExclusiveLockClause
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext
import dev.s7a.sqldelight.check.rule.api.findSourcePattern
import dev.s7a.sqldelight.check.rule.api.reportSqlStatementMatches

/**
 * Reports MySQL ALTER TABLE statements that request an exclusive lock.
 *
 * Exclusive locks block concurrent access and are risky for migrations that run
 * against live databases.
 */
public class NoExclusiveLockRule : Rule {
    override val id: RuleId = RuleId("no-exclusive-lock")
    override val defaultSeverity: Severity = Severity.Error
    override val defaultEnable: Boolean = true
    override val targetDialect: DialectId = MySqlDialectId

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        reportSqlStatementMatches(
            context = context,
            reporter = reporter,
            message = "Avoid MySQL ALTER TABLE LOCK=EXCLUSIVE for online migrations.",
            hashLineComments = true,
        ) { statement ->
            statement.findSourcePattern(ExclusiveLockClause, context.database.dialect.sourcePatterns)
        }
    }
}
