package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SqlDialectSourceTerm
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
        val parenthesisDepths = content.computeParenthesisDepths()
        tokens.forEachIndexed { index, token ->
            if (!token.isTerm(SqlDialectSourceTerm.Delete)) return@forEachIndexed
            if (isReferentialDeleteAction(tokens, index, parenthesisDepths)) return@forEachIndexed
            val depth = parenthesisDepths[token.startOffset]
            val statementEnd = content.statementEndAfter(token.startOffset)
            if (hasWhereClauseAfter(tokens, index, statementEnd, depth, parenthesisDepths)) return@forEachIndexed

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

private fun isReferentialDeleteAction(
    tokens: List<SqlToken>,
    deleteIndex: Int,
    parenthesisDepths: IntArray,
): Boolean {
    val previous = tokens.getOrNull(deleteIndex - 1) ?: return false
    val delete = tokens[deleteIndex]
    return previous.isTerm(SqlDialectSourceTerm.On) &&
        parenthesisDepths[previous.startOffset] == parenthesisDepths[delete.startOffset]
}
