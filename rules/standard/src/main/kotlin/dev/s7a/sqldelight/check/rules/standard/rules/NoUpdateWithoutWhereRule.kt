package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports UPDATE statements without a top-level WHERE clause.
 */
public class NoUpdateWithoutWhereRule : Rule {
    override val id: RuleId = RuleId("standard:no-update-without-where")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnablement: Enablement = Enablement.Auto

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        val tokens = content.sqlTokens().toList()
        tokens.forEachIndexed { index, token ->
            if (!token.isKeyword("update")) return@forEachIndexed
            val depth = content.sqlParenthesisDepthAt(token.startOffset)
            val statementEnd = content.statementEndAfter(token.startOffset)
            val hasWhere =
                tokens
                    .drop(index + 1)
                    .takeWhile { candidate -> candidate.startOffset < statementEnd }
                    .any { candidate -> candidate.isKeyword("where") && content.sqlParenthesisDepthAt(candidate.startOffset) == depth }
            if (hasWhere) return@forEachIndexed

            reporter.report(
                Diagnostic(
                    ruleId = id,
                    severity = defaultSeverity,
                    message = "UPDATE statements should include a WHERE clause.",
                    file = context.file,
                    range = content.rangeAtOffsets(token.startOffset, token.endOffset),
                    database = context.database,
                ),
            )
        }
    }
}
