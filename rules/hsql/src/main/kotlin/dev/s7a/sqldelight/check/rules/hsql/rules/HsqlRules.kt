package dev.s7a.sqldelight.check.rules.hsql.rules

import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.DialectCapabilities
import dev.s7a.sqldelight.check.api.DialectCapability
import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

private val regexOptions = setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE, RegexOption.DOT_MATCHES_ALL)

/**
 * Reports HSQL database or file settings in SQLDelight schema and migration sources.
 *
 * These settings change database-wide operational behavior and are safer to keep
 * in database bootstrap or administration code instead of versioned DDL files.
 */
public class NoDatabaseFileSettingsRule : RegexHsqlRule(
    ruleName = "no-database-file-settings",
    pattern = """\bSET\s+(?:DATABASE|FILES)\b""",
    message = "Keep HSQL database and file settings out of SQLDelight schema migrations.",
)

/**
 * Reports HSQL system operations in SQLDelight schema and migration sources.
 *
 * Operations such as CHECKPOINT, SHUTDOWN, BACKUP, SCRIPT, and bulk import or
 * export are administrative actions rather than schema changes.
 */
public class NoSystemOperationsRule : RegexHsqlRule(
    ruleName = "no-system-operations",
    pattern = """\b(?:CHECKPOINT|SHUTDOWN)\b|(?:^|;)\s*SCRIPT\b|\bBACKUP\s+DATABASE\b|\bPERFORM\s+(?:EXPORT|IMPORT)\b""",
    message = "Keep HSQL system operations out of SQLDelight schema migrations.",
)

/**
 * Reports HSQL TEXT table declarations and source bindings.
 *
 * TEXT tables depend on external delimited files, which makes schema migrations
 * sensitive to runtime file layout and database file-access policy.
 */
public class NoTextTableSourceRule : RegexHsqlRule(
    ruleName = "no-text-table-source",
    pattern = """\bCREATE\s+TEXT\s+TABLE\b|\bSET\s+TABLE\b(?:(?!;).)*\bSOURCE\b""",
    message = "Avoid HSQL TEXT table file sources in SQLDelight schema migrations.",
)

/**
 * Base implementation for HSQL rules that can be evaluated from masked source text.
 *
 * Subclasses provide the rule id suffix, regex pattern, and diagnostic message
 * while this class handles HSQL capability gating and diagnostic ranges.
 */
public abstract class RegexHsqlRule(
    ruleName: String,
    pattern: String,
    private val message: String,
) : Rule {
    override val id: RuleId = RuleId("hsql:$ruleName")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnablement: Enablement = Enablement.Auto
    override val targetCapability: DialectCapability = DialectCapabilities.Hsql
    private val regex = Regex(pattern, regexOptions)

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        if (!isApplicable(context)) return
        val content = context.file.content
        val masked = content.maskSqlCommentsAndQuotedText()
        regex.findAll(masked).forEach { match ->
            reporter.report(
                Diagnostic(
                    ruleId = id,
                    severity = defaultSeverity,
                    message = message,
                    file = context.file,
                    range = content.rangeAtOffsets(match.range.first, match.range.last + 1),
                    database = context.database,
                ),
            )
        }
    }
}
