package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports table aliases that omit the `AS` keyword.
 *
 * Explicit table alias syntax makes table references easier to scan and keeps
 * alias style consistent across ordinary tables and derived tables.
 */
public class RequireTableAliasAsRule : Rule {
    override val id: RuleId = RuleId("require-table-alias-as")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        content.tableReferences().forEach { reference ->
            val alias = reference.alias ?: return@forEach
            if (reference.aliasUsesAs) return@forEach

            reporter.report(
                RuleDiagnostic(
                    severity = defaultSeverity,
                    message = "Table aliases should use AS.",
                    file = context.file,
                    range = content.rangeAtOffsets(alias.startOffset, alias.endOffset),
                    database = context.database,
                ),
            )
        }
    }
}
