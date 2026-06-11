package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SourceFileKind
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext
import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

/**
 * Reports duplicate SQLDelight query or grouped statement labels in one file.
 */
public class NoDuplicateQueryLabelRule : Rule {
    override val id: RuleId = RuleId("no-duplicate-query-label")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        if (context.file.kind != SourceFileKind.Query) return

        val content = context.file.content
        val seen = mutableSetOf<String>()
        content.sharedSqlDelightLabels().forEach { label ->
            if (seen.add(label.name)) return@forEach
            reporter.report(
                RuleDiagnostic(
                    severity = defaultSeverity,
                    message = "SQLDelight label '${label.name}' is duplicated in this file.",
                    file = context.file,
                    range = content.rangeAtOffsets(label.startOffset, label.endOffset),
                    database = context.database,
                ),
            )
        }
    }
}
