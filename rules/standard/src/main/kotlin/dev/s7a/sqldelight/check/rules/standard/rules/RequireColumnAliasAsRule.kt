package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports SELECT result column aliases that omit the `AS` keyword.
 *
 * Explicit result alias syntax avoids ambiguous computed targets and keeps
 * generated SQLDelight result column names visible.
 */
public class RequireColumnAliasAsRule : Rule {
    override val id: RuleId = RuleId("standard:require-column-alias-as")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnablement: Enablement = Enablement.Auto

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        content.resultColumnAliases()
            .filterNot { alias -> alias.usesAs }
            .forEach { alias ->
                reporter.report(
                    Diagnostic(
                        ruleId = id,
                        severity = defaultSeverity,
                        message = "Column aliases should use AS.",
                        file = context.file,
                        range = content.rangeAtOffsets(alias.token.startOffset, alias.token.endOffset),
                        database = context.database,
                    ),
                )
            }
    }
}
