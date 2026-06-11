package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports INSERT statements that rely on table column order.
 */
public class PreferExplicitColumnListInInsertRule : Rule {
    override val id: RuleId = RuleId("prefer-explicit-column-list-in-insert")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        val tokens = content.sqlTokens().toList()
        tokens.forEachIndexed { index, token ->
            if (!token.isKeyword("insert")) return@forEachIndexed
            val into = tokens.getOrNull(index + 1)?.takeIf { it.isKeyword("into") } ?: return@forEachIndexed
            val table = tokens.getOrNull(index + 2) ?: return@forEachIndexed
            val values =
                tokens
                    .drop(index + 3)
                    .firstOrNull { candidate ->
                        candidate.startOffset < content.statementEndAfter(token.startOffset) && candidate.isKeyword("values")
                    } ?: return@forEachIndexed
            val betweenTableAndValues =
                content.sqlCharacters()
                    .dropWhile { character -> character.offset < table.endOffset }
                    .takeWhile { character -> character.offset < values.startOffset }
                    .toList()
            if (betweenTableAndValues.any { character -> character.value == '(' }) return@forEachIndexed

            reporter.report(
                RuleDiagnostic(
                    severity = defaultSeverity,
                    message = "INSERT statements should include an explicit column list.",
                    file = context.file,
                    range = content.rangeAtOffsets(into.startOffset, values.endOffset),
                    database = context.database,
                ),
            )
        }
    }
}
