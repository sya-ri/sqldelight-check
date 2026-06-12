package dev.s7a.sqldelight.check.rules.mysql.rules

import dev.s7a.sqldelight.check.api.DialectId
import dev.s7a.sqldelight.check.dialects.mysql.MySqlDialectId
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.dialects.mysql.CopyAlgorithmClause
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext
import dev.s7a.sqldelight.check.rule.api.findSourcePattern
import dev.s7a.sqldelight.check.rule.api.reportSqlStatementMatches

/**
 * Reports MySQL ALTER TABLE statements that force the COPY algorithm.
 *
 * COPY can rebuild the table and is usually unsafe for online schema changes.
 */
public class NoCopyAlgorithmRule : Rule {
    override val id: RuleId = RuleId("no-copy-algorithm")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true
    override val targetDialect: DialectId = MySqlDialectId

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        reportSqlStatementMatches(
            context = context,
            reporter = reporter,
            message = "Avoid MySQL ALTER TABLE ALGORITHM=COPY for online migrations.",
            hashLineComments = true,
        ) { statement ->
            statement.findSourcePattern(CopyAlgorithmClause, context.database.dialect.sourcePatterns)
        }
    }
}
