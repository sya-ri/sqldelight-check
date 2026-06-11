package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.Fix
import dev.s7a.sqldelight.check.api.FixSafety
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.TextEdit
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports SQL literal tokens that are not uppercase.
 */
public class LiteralCaseRule : Rule {
    override val id: RuleId = RuleId("literal-case")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        content
            .sqlTokens()
            .filter { token -> token.text.lowercase() in literals }
            .filterNot { token -> token.text == token.text.uppercase() }
            .forEach { token ->
                val range = content.rangeAtOffsets(token.startOffset, token.endOffset)
                reporter.report(
                    RuleDiagnostic(
                        severity = defaultSeverity,
                        message = "SQL literal '${token.text}' should be uppercase.",
                        file = context.file,
                        range = range,
                        database = context.database,
                        fixes =
                            listOf(
                                Fix(
                                    title = "Uppercase literal",
                                    safety = FixSafety.Unsafe,
                                    edits = listOf(TextEdit(range = range, replacement = token.text.uppercase())),
                                ),
                            ),
                    ),
                )
            }
    }

    private companion object {
        val literals = setOf("false", "null", "true")
    }
}
