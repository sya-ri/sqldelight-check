package dev.s7a.sqldelight.check.rules.hsql.rules

import dev.s7a.sqldelight.check.api.DialectCapability
import dev.s7a.sqldelight.check.dialects.hsql.HsqlDialectCapability
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.dialects.hsql.TextTableSourceBindingStart
import dev.s7a.sqldelight.check.dialects.hsql.TextTableSourceClause
import dev.s7a.sqldelight.check.dialects.hsql.TextTableSourceStatement
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext
import dev.s7a.sqldelight.check.rule.api.findSourcePattern
import dev.s7a.sqldelight.check.rule.api.findSourcePatternsInOrder
import dev.s7a.sqldelight.check.rule.api.reportSqlStatementMatches

/**
 * Reports HSQL TEXT table declarations and source bindings.
 *
 * TEXT tables depend on external delimited files, which makes schema migrations
 * sensitive to runtime file layout and database file-access policy.
 */
public class NoTextTableSourceRule : Rule {
    override val id: RuleId = RuleId("no-text-table-source")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true
    override val targetCapability: DialectCapability = HsqlDialectCapability

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        reportSqlStatementMatches(
            context = context,
            reporter = reporter,
            message = "Avoid HSQL TEXT table file sources in SQLDelight schema migrations.",
        ) { statement ->
            statement.findSourcePattern(TextTableSourceStatement, context.database.dialect.sourcePatterns)
                ?: statement.findSourcePatternsInOrder(
                    context.database.dialect.sourcePatterns,
                    TextTableSourceBindingStart,
                    TextTableSourceClause,
                )
        }
    }
}
