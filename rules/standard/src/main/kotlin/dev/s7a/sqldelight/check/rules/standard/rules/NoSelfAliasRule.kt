package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports table aliases that repeat the table name they alias.
 *
 * Self-aliases add noise without making later column references clearer.
 */
public class NoSelfAliasRule : Rule {
    override val id: RuleId = RuleId("no-self-alias")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        context.file.content.tableReferences(context.database.dialect.sourceKeywords).forEach { reference ->
            val tableName = reference.tableName ?: return@forEach
            val alias = reference.alias ?: return@forEach
            if (!tableName.equals(alias.text, ignoreCase = true)) return@forEach

            reporter.report(
                RuleDiagnostic(
                    severity = defaultSeverity,
                    message = "Table aliases should not repeat the table name.",
                    file = context.file,
                    range = context.file.content.rangeAtOffsets(alias.startOffset, alias.endOffset),
                    database = context.database,
                ),
            )
        }
    }
}
