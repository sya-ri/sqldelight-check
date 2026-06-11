package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports comma tokens that lead a line in multiline SQL.
 */
public class NoLeadingCommaRule : Rule {
    override val id: String = "no-leading-comma"
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        if (!content.contains('\n')) return

        val lines = content.linesWithRanges()
        content
            .sqlCharacters()
            .filter { character -> character.value == ',' }
            .forEach { comma ->
                val line = lines.lineContaining(comma.offset) ?: return@forEach
                if (line.firstNonWhitespaceOffset != comma.offset) return@forEach

                reporter.report(
                    Diagnostic(
                        ruleId = RuleId(id),
                        severity = defaultSeverity,
                        message = "Commas should trail the previous line.",
                        file = context.file,
                        range = content.rangeAtOffsets(comma.offset, comma.offset + 1),
                        database = context.database,
                    ),
                )
            }
    }
}
