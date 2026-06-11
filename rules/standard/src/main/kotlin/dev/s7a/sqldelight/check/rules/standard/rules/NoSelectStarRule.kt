package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

import dev.s7a.sqldelight.check.api.RuleDiagnostic
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
        content.selectFromRanges().forEach { select ->
            content.sqlCharacters()
                .dropWhile { character -> character.offset <= select.selectEndOffset }
                .takeWhile { character -> character.offset < select.fromStartOffset }
                .filter { character -> character.value == '*' && content.sqlParenthesisDepthAt(character.offset) == select.depth }
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
