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
 * Reports repeated semicolon tokens with only whitespace between them.
 */
public class NoRedundantSemicolonsRule : Rule {
    override val id: RuleId = RuleId("no-redundant-semicolons")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        val semicolons = content.sqlCharacters().filter { character -> character.value == ';' }.toList()
        var index = 0
        while (index < semicolons.lastIndex) {
            val first = semicolons[index]
            var previous = first
            var lastRedundantIndex = index
            while (
                lastRedundantIndex + 1 < semicolons.size &&
                content.isWhitespaceOnly(previous.offset + 1, semicolons[lastRedundantIndex + 1].offset)
            ) {
                lastRedundantIndex++
                previous = semicolons[lastRedundantIndex]
            }

            if (lastRedundantIndex == index) {
                index++
                continue
            }

            val lastRedundant = semicolons[lastRedundantIndex]
            val fixEnd = content.horizontalWhitespaceEndAfter(lastRedundant.offset + 1)
            val range = content.rangeAtOffsets(first.offset + 1, fixEnd)
            reporter.report(
                RuleDiagnostic(
                    severity = defaultSeverity,
                    message = "Statement should not contain redundant semicolons.",
                    file = context.file,
                    range = range,
                    database = context.database,
                    fixes =
                        listOf(
                            Fix(
                                title = "Remove redundant semicolons",
                                safety = FixSafety.Safe,
                                edits = listOf(TextEdit(range = range, replacement = "")),
                            ),
                        ),
                ),
            )
            index = lastRedundantIndex + 1
        }
    }
}

private fun String.isWhitespaceOnly(
    startOffset: Int,
    endOffset: Int,
): Boolean {
    var index = startOffset
    while (index < endOffset) {
        if (!this[index].isWhitespace()) return false
        index++
    }
    return true
}
