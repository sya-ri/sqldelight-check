package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports result-column wildcards in SELECT lists.
 */
public class NoSelectStarRule : Rule {
    override val id: RuleId = RuleId("no-select-star")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        val tokens = content.sqlTokens().toList()
        tokens.forEachIndexed { index, token ->
            if (!token.isKeyword("select")) return@forEachIndexed
            val selectDepth = content.sqlParenthesisDepthAt(token.startOffset)
            val statementEnd = content.statementEndAfter(token.startOffset)
            val fromToken =
                tokens
                    .drop(index + 1)
                    .firstOrNull { candidate ->
                        candidate.startOffset < statementEnd &&
                            content.sqlParenthesisDepthAt(candidate.startOffset) == selectDepth &&
                            candidate.isKeyword("from")
                    } ?: return@forEachIndexed
            content.sqlCharacters()
                .dropWhile { character -> character.offset <= token.endOffset }
                .takeWhile { character -> character.offset < fromToken.startOffset }
                .filter { character -> character.value == '*' && content.sqlParenthesisDepthAt(character.offset) == selectDepth }
                .forEach { character ->
                    reporter.report(
                        RuleDiagnostic(
                            severity = defaultSeverity,
                            message = "Avoid SELECT * and list result columns explicitly.",
                            file = context.file,
                            range = content.rangeAtOffsets(character.offset, character.offset + 1),
                            database = context.database,
                        ),
                    )
                }
        }
    }
}
