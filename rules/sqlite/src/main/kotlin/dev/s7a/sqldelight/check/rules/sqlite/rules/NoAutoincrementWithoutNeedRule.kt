package dev.s7a.sqldelight.check.rules.sqlite.rules

import dev.s7a.sqldelight.check.api.DialectCapability
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.AutoincrementKeyword
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext
import dev.s7a.sqldelight.check.rule.api.findSourcePattern
import dev.s7a.sqldelight.check.rule.api.reportSqlStatementMatches

/**
 * Reports SQLite AUTOINCREMENT usage for schemas that do not need its stricter behavior.
 *
 * AUTOINCREMENT changes rowid reuse semantics and can add overhead compared with
 * the normal SQLite rowid allocator.
 */
public class NoAutoincrementWithoutNeedRule : Rule {
    override val id: RuleId = RuleId("no-autoincrement-without-need")
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
            message = "Avoid SQLite AUTOINCREMENT unless its stricter rowid behavior is required.",
        ) { statement ->
            statement.findSourcePattern(AutoincrementKeyword, context.database.dialect.sourcePatterns)
        }
    }
}
