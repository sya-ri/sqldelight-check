package dev.s7a.sqldelight.check.rules.mysql.rules

import dev.s7a.sqldelight.check.api.DialectCapability
import dev.s7a.sqldelight.check.dialects.mysql.MySqlDialectCapability
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.dialects.mysql.ReplaceIntoStatementStart
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext
import dev.s7a.sqldelight.check.rule.api.findSourcePattern
import dev.s7a.sqldelight.check.rule.api.reportSqlTokenMatch
import dev.s7a.sqldelight.check.rule.api.sqlTokens

/**
 * Reports MySQL REPLACE INTO statements.
 *
 * `REPLACE` deletes conflicting rows before inserting, which can trigger
 * cascading side effects that are easy to miss in application code.
 */
public class NoReplaceIntoRule : Rule {
    override val id: RuleId = RuleId("no-replace-into")
    override val defaultSeverity: Severity = Severity.Error
    override val defaultEnable: Boolean = true
    override val targetCapability: DialectCapability = MySqlDialectCapability

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        if (!isApplicable(context)) return

        val content = context.file.content
        val tokens = content.sqlTokens(hashLineComments = true).toList()
        tokens.findSourcePattern(ReplaceIntoStatementStart, context.database.dialect.sourcePatterns)
            ?.let { match ->
                reportSqlTokenMatch(
                    context = context,
                    reporter = reporter,
                    message =
                        "Avoid MySQL REPLACE INTO because it can delete and insert rows instead of updating them.",
                    match = match,
                )
            }
    }
}
