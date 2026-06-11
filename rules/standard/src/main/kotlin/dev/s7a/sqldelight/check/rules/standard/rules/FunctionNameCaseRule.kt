package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.Fix
import dev.s7a.sqldelight.check.api.FixSafety
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.TextEdit
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports common SQL function names that are not uppercase.
 */
public class FunctionNameCaseRule : Rule {
    override val id: RuleId = RuleId("function-name-case")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        content
            .sqlTokens()
            .filter { token -> token.text.lowercase() in commonSqlFunctions }
            .filter { token -> content.nextNonHorizontalWhitespace(token.endOffset) == '(' }
            .filterNot { token -> token.text == token.text.uppercase() }
            .forEach { token ->
                val range = content.rangeAtOffsets(token.startOffset, token.endOffset)
                reporter.report(
                    RuleDiagnostic(
                        severity = defaultSeverity,
                        message = "SQL function '${token.text}' should be uppercase.",
                        file = context.file,
                        range = range,
                        database = context.database,
                        fixes =
                            listOf(
                                Fix(
                                    title = "Uppercase function name",
                                    safety = FixSafety.Unsafe,
                                    edits = listOf(TextEdit(range = range, replacement = token.text.uppercase())),
                                ),
                            ),
                    ),
                )
            }
    }

}
