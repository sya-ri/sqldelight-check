package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports DELETE statements without a top-level WHERE clause.
 */
public class NoDeleteWithoutWhereRule : Rule {
    override val id: RuleId = RuleId("no-delete-without-where")
    override val defaultSeverity: Severity = Severity.Error
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        val tokens = content.sqlTokens().toList()
        tokens.forEachIndexed { index, token ->
            if (!token.isKeyword("delete")) return@forEachIndexed
            if (content.isReferentialDeleteAction(tokens, index)) return@forEachIndexed
            val depth = content.sqlParenthesisDepthAt(token.startOffset)
            val statementEnd = content.statementEndAfter(token.startOffset)
            if (content.hasWhereClauseAfter(tokens, index, statementEnd, depth)) return@forEachIndexed

            reporter.report(
                RuleDiagnostic(
                    severity = defaultSeverity,
                    message = "DELETE statements should include a WHERE clause.",
                    file = context.file,
                    range = content.rangeAtOffsets(token.startOffset, token.endOffset),
                    database = context.database,
                ),
            )
        }
    }
}

private fun String.isReferentialDeleteAction(
    tokens: List<SqlToken>,
    deleteIndex: Int,
): Boolean {
    val previous = tokens.getOrNull(deleteIndex - 1) ?: return false
    val delete = tokens[deleteIndex]
    return previous.isKeyword("on") &&
        sqlParenthesisDepthAt(previous.startOffset) == sqlParenthesisDepthAt(delete.startOffset)
}
