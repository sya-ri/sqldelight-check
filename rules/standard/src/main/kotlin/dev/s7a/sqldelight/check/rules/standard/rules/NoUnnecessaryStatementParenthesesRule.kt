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
 * Reports redundant statement-level parentheses around top-level `SELECT` statements.
 */
public class NoUnnecessaryStatementParenthesesRule : Rule {
    override val id: RuleId = RuleId("no-unnecessary-statement-parentheses")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        val tokens = content.sqlTokens().toList()
        content.sqlCharacters()
            .filter { character -> character.value == '(' && content.sqlParenthesisDepthAt(character.offset) == 0 }
            .forEach { character ->
                val openOffset = character.offset
                if (!content.hasStatementBoundaryBefore(openOffset)) return@forEach

                val closeOffset = content.matchingClosingParenthesisOffset(openOffset) ?: return@forEach
                if (!content.hasStatementBoundaryAfter(closeOffset + 1)) return@forEach

                val select =
                    tokens.firstOrNull { token ->
                            token.startOffset in (openOffset + 1)..<closeOffset &&
                            token.isTerm(SqlDialectSourceTerm.Select)
                    } ?: return@forEach
                if (content.sqlParenthesisDepthAt(select.startOffset) != 1) return@forEach
                if (tokens.any { token -> token.startOffset in (openOffset + 1)..<select.startOffset }) return@forEach

                reporter.report(
                    RuleDiagnostic(
                        severity = defaultSeverity,
                        message = "Remove redundant parentheses around the SELECT statement.",
                        file = context.file,
                        range = content.rangeAtOffsets(openOffset, closeOffset + 1),
                        database = context.database,
                    ),
                )
            }
    }
}

private fun String.hasStatementBoundaryBefore(offset: Int): Boolean {
    val previous = previousSqlCharacterBefore(offset) ?: return true
    return previous.value == ';' || previous.value == ':'
}

private fun String.hasStatementBoundaryAfter(offset: Int): Boolean {
    val next = nextSqlCharacterAfter(offset) ?: return true
    return next.value == ';'
}
