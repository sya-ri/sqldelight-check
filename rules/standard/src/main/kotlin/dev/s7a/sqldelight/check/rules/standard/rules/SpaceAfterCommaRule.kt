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
 * Reports missing or repeated spaces after comma tokens on the same line.
 */
public class SpaceAfterCommaRule : Rule {
    override val id: RuleId = RuleId("space-after-comma")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        content
            .sqlCharacters()
            .filter { character -> character.value == ',' }
            .forEach { comma ->
                val whitespaceEnd = content.horizontalWhitespaceEndAfter(comma.offset + 1)
                if (whitespaceEnd >= content.length) return@forEach
                val next = content[whitespaceEnd]
                if (next == '\r' || next == '\n' || next in trailingCommaFollowers) return@forEach

                val currentWhitespace = content.substring(comma.offset + 1, whitespaceEnd)
                if (currentWhitespace == " ") return@forEach

                val range = content.rangeAtOffsets(comma.offset + 1, whitespaceEnd)
                reporter.report(
                    RuleDiagnostic(
                        severity = defaultSeverity,
                        message = "Comma should be followed by one space.",
                        file = context.file,
                        range = range,
                        database = context.database,
                        fixes =
                            listOf(
                                Fix(
                                    title = "Use one space after comma",
                                    safety = FixSafety.Safe,
                                    edits = listOf(TextEdit(range = range, replacement = " ")),
                                ),
                            ),
                    ),
                )
            }
    }

    private companion object {
        val trailingCommaFollowers = setOf(')', ']', '}', ';', ',')
    }
}
