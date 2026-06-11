package dev.s7a.sqldelight.check.rule.api

import dev.s7a.sqldelight.check.api.DialectCapability
import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity

private val defaultRegexOptions = setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE, RegexOption.DOT_MATCHES_ALL)

/**
 * Base implementation for rules that report regular expression matches in masked SQL source text.
 *
 * Comments and quoted text are replaced with spaces before matching, so diagnostic
 * ranges still point at the original source offsets.
 */
public abstract class RegexRule(
    ruleName: String,
    pattern: String,
    private val message: String,
    override val defaultSeverity: Severity = Severity.Warning,
    override val defaultEnable: Boolean = true,
    override val targetCapability: DialectCapability? = null,
    private val hashLineComments: Boolean = false,
) : Rule {
    override val id: RuleId = RuleId(ruleName)
    private val regex = Regex(pattern, defaultRegexOptions)

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        if (!isApplicable(context)) return
        val content = context.file.content
        val masked = content.maskSqlCommentsAndQuotedText(hashLineComments = hashLineComments)
        regex.findAll(masked).filter(::shouldReport).forEach { match ->
            reporter.report(
                RuleDiagnostic(
                    severity = defaultSeverity,
                    message = message,
                    file = context.file,
                    range = content.rangeAtOffsets(match.range.first, match.range.last + 1),
                    database = context.database,
                ),
            )
        }
    }

    /**
     * Returns whether a regex match should be reported.
     *
     * Subclasses can override this for simple same-match exclusions while
     * keeping the standard masking, range mapping, and diagnostic reporting.
     */
    protected open fun shouldReport(match: MatchResult): Boolean = true
}
