package com.example.sqldelight.rules

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SourceFileKind
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext
import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

public class NoSelectStarRule : Rule {
    override val id: RuleId = RuleId("no-select-star")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun isApplicable(context: RuleContext): Boolean =
        context.file.kind == SourceFileKind.Query

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        Regex("""(?i)\bselect\s+\*""")
            .findAll(content)
            .forEach { match ->
                val starOffset = match.value.lastIndexOf('*').let { offset -> match.range.first + offset }
                reporter.report(
                    RuleDiagnostic(
                        severity = defaultSeverity,
                        message = context.options["message"] ?: "Avoid SELECT * in public SQLDelight queries.",
                        file = context.file,
                        range = content.rangeAtOffsets(starOffset, starOffset + 1),
                        database = context.database,
                    ),
                )
            }
    }
}
