package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.Fix
import dev.s7a.sqldelight.check.api.FixSafety
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole
import dev.s7a.sqldelight.check.api.TextEdit
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports spaces or tabs between common SQL function names and their opening parenthesis.
 */
public class NoSpaceBeforeFunctionParenthesisRule : Rule {
    override val id: RuleId = RuleId("no-space-before-function-parenthesis")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        content
            .sqlTokens()
            .filter { token -> token.matches(context.database.dialect.sourcePatterns, SqlDialectSourcePatternRole.CommonFunctionName) }
            .forEach { token ->
                val parenthesisOffset = content.nextNonHorizontalWhitespaceOffset(token.endOffset) ?: return@forEach
                if (content[parenthesisOffset] != '(' || parenthesisOffset == token.endOffset) return@forEach

                val range = content.rangeAtOffsets(token.endOffset, parenthesisOffset)
                reporter.report(
                    RuleDiagnostic(
                        severity = defaultSeverity,
                        message = "Function name should not be separated from opening parenthesis.",
                        file = context.file,
                        range = range,
                        database = context.database,
                        fixes =
                            listOf(
                                Fix(
                                    title = "Remove whitespace before function parenthesis",
                                    safety = FixSafety.Safe,
                                    edits = listOf(TextEdit(range = range, replacement = "")),
                                ),
                            ),
                    ),
                )
            }
    }
}
