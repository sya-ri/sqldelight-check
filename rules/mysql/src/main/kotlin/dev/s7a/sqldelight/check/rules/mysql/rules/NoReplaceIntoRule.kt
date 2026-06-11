package dev.s7a.sqldelight.check.rules.mysql.rules

import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.DialectCapabilities
import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports MySQL REPLACE INTO statements.
 */
public class NoReplaceIntoRule : Rule {
    override val id: RuleId = RuleId("mysql:no-replace-into")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnablement: Enablement = Enablement.Auto

    override fun isApplicable(context: RuleContext): Boolean =
        DialectCapabilities.MySql in context.database.dialect.capabilities

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        if (!isApplicable(context)) return

        val content = context.file.content
        val tokens = content.sqlTokens().toList()
        tokens.forEachIndexed { index, token ->
            if (!token.isKeyword("replace")) return@forEachIndexed

            val intoToken = tokens.getOrNull(index + 1)
            if (intoToken?.isKeyword("into") != true) return@forEachIndexed

            reporter.report(
                Diagnostic(
                    ruleId = id,
                    severity = defaultSeverity,
                    message =
                        "Avoid MySQL REPLACE INTO because it can delete and insert rows instead of updating them.",
                    file = context.file,
                    range = content.rangeAtOffsets(token.startOffset, intoToken.endOffset),
                    database = context.database,
                ),
            )
        }
    }
}
