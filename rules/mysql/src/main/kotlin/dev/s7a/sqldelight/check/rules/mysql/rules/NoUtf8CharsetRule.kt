package dev.s7a.sqldelight.check.rules.mysql.rules

import dev.s7a.sqldelight.check.api.DialectCapability
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.LegacyUtf8CharsetDeclaration
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext
import dev.s7a.sqldelight.check.rule.api.findSourcePattern
import dev.s7a.sqldelight.check.rule.api.reportSqlStatementMatches

/**
 * Reports MySQL character set declarations that use utf8 instead of utf8mb4.
 *
 * MySQL `utf8` is an alias for `utf8mb3`, so this rule points schemas toward
 * the full Unicode `utf8mb4` character set.
 */
public class NoUtf8CharsetRule : Rule {
    override val id: RuleId = RuleId("no-utf8-charset")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true
    override val targetCapability: DialectCapability = DialectCapability.MySql

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        reportSqlStatementMatches(
            context = context,
            reporter = reporter,
            message = "Use utf8mb4 instead of MySQL utf8.",
            hashLineComments = true,
        ) { statement ->
            statement.findSourcePattern(LegacyUtf8CharsetDeclaration, context.database.dialect.sourcePatterns)
        }
    }
}
