package dev.s7a.sqldelight.check.rules.mysql.rules

import dev.s7a.sqldelight.check.api.DialectCapability
import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext
import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

private val zeroDateRegexOptions = setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE, RegexOption.DOT_MATCHES_ALL)

/**
 * Reports MySQL zero date defaults that are rejected by stricter SQL modes.
 *
 * The rule helps avoid schema defaults that fail under `NO_ZERO_DATE` or related
 * production SQL modes.
 */
public class NoZeroDateDefaultRule : Rule {
    override val id: RuleId = RuleId("no-zero-date-default")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true
    override val targetCapability: DialectCapability = DialectCapability.MySql
    private val regex = Regex("""\bDEFAULT\s+['"]0000-00-00(?:\s+\d\d:\d\d:\d\d)?['"]""", zeroDateRegexOptions)

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        if (!isApplicable(context)) return
        val content = context.file.content
        regex.findAll(content).forEach { match ->
            reporter.report(
                RuleDiagnostic(
                    severity = defaultSeverity,
                    message = "Avoid zero date defaults in MySQL schemas.",
                    file = context.file,
                    range = content.rangeAtOffsets(match.range.first, match.range.last + 1),
                    database = context.database,
                ),
            )
        }
    }
}
