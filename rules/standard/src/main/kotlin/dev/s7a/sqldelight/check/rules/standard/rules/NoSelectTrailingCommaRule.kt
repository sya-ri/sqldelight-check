package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.Fix
import dev.s7a.sqldelight.check.api.FixSafety
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SqlDialectSourceTerm
import dev.s7a.sqldelight.check.api.TextEdit
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports trailing commas at the end of `SELECT` clauses.
 */
public class NoSelectTrailingCommaRule : Rule {
    override val id: RuleId = RuleId("no-select-trailing-comma")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        val tokens = content.sqlTokens().toList()
        tokens.forEachIndexed { index, token ->
            if (!token.isTerm(SqlDialectSourceTerm.Select)) return@forEachIndexed
            val statementEnd = content.statementEndAfter(token.startOffset)
            val from = tokens.firstTermAfter(index + 1, statementEnd, SqlDialectSourceTerm.From) ?: return@forEachIndexed
            val commaOffset = content.previousNonWhitespaceOffset(from.startOffset, ',') ?: return@forEachIndexed

            val range = content.rangeAtOffsets(commaOffset, commaOffset + 1)
            reporter.report(
                RuleDiagnostic(
                    severity = defaultSeverity,
                    message = "SELECT clause should not end with a trailing comma.",
                    file = context.file,
                    range = range,
                    database = context.database,
                    fixes =
                        listOf(
                            Fix(
                                title = "Remove trailing SELECT comma",
                                safety = FixSafety.Unsafe,
                                edits = listOf(TextEdit(range = range, replacement = "")),
                            ),
                        ),
                ),
            )
        }
    }
}
