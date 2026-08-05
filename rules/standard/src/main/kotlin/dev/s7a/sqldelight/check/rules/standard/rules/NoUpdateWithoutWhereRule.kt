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
 * Reports UPDATE statements without a top-level WHERE clause.
 */
public class NoUpdateWithoutWhereRule : Rule {
    override val id: RuleId = RuleId("no-update-without-where")
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
            if (!token.isTerm(SqlDialectSourceTerm.Update)) return@forEachIndexed
            if (isUpsertUpdateAction(tokens, index, parenthesisDepths)) return@forEachIndexed
            val depth = parenthesisDepths[token.startOffset]
            val statementEnd = content.statementEndAfter(token.startOffset)
            if (hasWhereClauseAfter(tokens, index, statementEnd, depth, parenthesisDepths)) return@forEachIndexed

            reporter.report(
                RuleDiagnostic(
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

private fun isUpsertUpdateAction(
    tokens: List<SqlToken>,
    updateIndex: Int,
    parenthesisDepths: IntArray,
): Boolean {
    val previous = tokens.getOrNull(updateIndex - 1) ?: return false
    val update = tokens[updateIndex]
    return previous.isTerm(SqlDialectSourceTerm.Do) &&
        parenthesisDepths[previous.startOffset] == parenthesisDepths[update.startOffset]
}
