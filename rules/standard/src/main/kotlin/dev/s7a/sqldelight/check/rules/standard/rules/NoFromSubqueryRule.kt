package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports top-level `FROM` and `JOIN` subqueries that should be written as CTEs.
 */
public class NoFromSubqueryRule : Rule {
    override val id: String = "no-from-subquery"
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        val tokens = content.sqlTokens().toList()
        tokens.forEach { token ->
            if (!token.isKeyword("from") && !token.isKeyword("join")) return@forEach
            if (content.sqlParenthesisDepthAt(token.startOffset) != 0) return@forEach

            val open = content.nextSqlCharacterAfter(token.endOffset) ?: return@forEach
            if (open.value != '(') return@forEach
            val closeOffset = content.matchingClosingParenthesisOffset(open.offset) ?: return@forEach
            val select =
                tokens.firstOrNull { candidate ->
                    candidate.startOffset > open.offset &&
                        candidate.startOffset < closeOffset &&
                        candidate.isKeyword("select")
                } ?: return@forEach
            if (content.sqlParenthesisDepthAt(select.startOffset) != 1) return@forEach
            if (tokens.any { candidate -> candidate.startOffset in (open.offset + 1)..<select.startOffset }) {
                return@forEach
            }

            reporter.report(
                Diagnostic(
                    ruleId = RuleId(id),
                    severity = defaultSeverity,
                    message = "Use a CTE instead of a top-level FROM or JOIN subquery.",
                    file = context.file,
                    range = content.rangeAtOffsets(open.offset, closeOffset + 1),
                    database = context.database,
                ),
            )
        }
    }
}
