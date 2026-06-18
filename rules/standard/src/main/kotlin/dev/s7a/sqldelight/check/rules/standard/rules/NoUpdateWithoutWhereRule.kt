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
        tokens.forEachIndexed { index, token ->
            if (!token.isTerm(SqlDialectSourceTerm.Update)) return@forEachIndexed
            if (content.isUpsertUpdateAction(tokens, index)) return@forEachIndexed
            if (content.isCreateTriggerEvent(tokens, index)) return@forEachIndexed
            val depth = content.sqlParenthesisDepthAt(token.startOffset)
            val statementEnd = content.statementEndAfter(token.startOffset)
            if (content.hasWhereClauseAfter(tokens, index, statementEnd, depth)) return@forEachIndexed

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

private fun String.isUpsertUpdateAction(
    tokens: List<SqlToken>,
    updateIndex: Int,
): Boolean {
    val previous = tokens.getOrNull(updateIndex - 1) ?: return false
    val update = tokens[updateIndex]
    return previous.isTerm(SqlDialectSourceTerm.Do) &&
        sqlParenthesisDepthAt(previous.startOffset) == sqlParenthesisDepthAt(update.startOffset)
}

private fun String.isCreateTriggerEvent(
    tokens: List<SqlToken>,
    updateIndex: Int,
): Boolean {
    val update = tokens[updateIndex]
    val statementStart = lastIndexOf(';', update.startOffset).let { offset -> if (offset == -1) 0 else offset + 1 }
    val statementDepth = sqlParenthesisDepthAt(update.startOffset)
    val statementTokens =
        tokens
            .asSequence()
            .withIndex()
            .dropWhile { (_, token) -> token.startOffset < statementStart }
            .takeWhile { (index, _) -> index <= updateIndex }
            .filter { (_, token) -> sqlParenthesisDepthAt(token.startOffset) == statementDepth }
            .toList()
    if (statementTokens.size < 4) return false
    if (!statementTokens[0].value.isTerm(SqlDialectSourceTerm.Create)) return false
    if (!statementTokens[1].value.isTerm(SqlDialectSourceTerm.Trigger)) return false
    if (statementTokens.any { (_, token) -> token.isTerm(SqlDialectSourceTerm.Begin) }) return false

    val statementEnd = statementEndAfter(statementTokens[0].value.startOffset)
    return tokens
        .asSequence()
        .drop(updateIndex + 1)
        .takeWhile { token -> token.startOffset < statementEnd }
        .any { token ->
            token.isTerm(SqlDialectSourceTerm.On) &&
                sqlParenthesisDepthAt(token.startOffset) == statementDepth
        }
}
