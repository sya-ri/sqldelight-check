package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SourceFileKind
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext
import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

/**
 * Reports wildcard SQLDelight imports.
 */
public class NoWildcardImportRule : Rule {
    override val id: RuleId = RuleId("no-wildcard-import")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        if (context.file.kind != SourceFileKind.Query) return

        val content = context.file.content
        content.sqlDelightImports()
            .filter { import -> import.name.endsWith(".*") }
            .forEach { import ->
                reporter.report(
                    RuleDiagnostic(
                        severity = defaultSeverity,
                        message = "Avoid wildcard SQLDelight imports.",
                        file = context.file,
                        range = content.rangeAtOffsets(import.nameStartOffset, import.nameEndOffset),
                        database = context.database,
                    ),
                )
            }
    }
}
