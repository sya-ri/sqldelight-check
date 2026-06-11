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
 * Reports adjacent repeated semicolon tokens.
 */
public class NoConsecutiveSemicolonsRule : Rule {
    override val id: RuleId = RuleId("no-consecutive-semicolons")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        var reportedRunEnd = -1
        content
            .sqlCharacters()
            .filter { character -> character.value == ';' }
            .forEach { semicolon ->
                if (semicolon.offset < reportedRunEnd) return@forEach
                if (semicolon.offset + 1 >= content.length || content[semicolon.offset + 1] != ';') return@forEach

                var runEnd = semicolon.offset + 1
                while (runEnd < content.length && content[runEnd] == ';') {
                    runEnd++
                }
                reportedRunEnd = runEnd

                val range = content.rangeAtOffsets(semicolon.offset + 1, runEnd)
                reporter.report(
                    RuleDiagnostic(
                        severity = defaultSeverity,
                        message = "Statement should not contain consecutive semicolons.",
                        file = context.file,
                        range = range,
                        database = context.database,
                        fixes =
                            listOf(
                                Fix(
                                    title = "Remove extra semicolons",
                                    safety = FixSafety.Safe,
                                    edits = listOf(TextEdit(range = range, replacement = "")),
                                ),
                            ),
                    ),
                )
            }
    }
}
