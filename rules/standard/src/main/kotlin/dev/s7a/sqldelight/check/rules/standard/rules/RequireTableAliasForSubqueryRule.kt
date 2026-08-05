package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports `FROM` and `JOIN` subqueries that do not declare a table alias.
 *
 * Aliasing derived tables keeps outer column references explicit.
 */
public class RequireTableAliasForSubqueryRule : Rule {
    override val id: RuleId = RuleId("require-table-alias-for-subquery")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        val structure = context.sourceStructure
        structure.topLevelSubqueryTableReferences(content, context.database.dialect.sourcePatterns).forEach { reference ->
            if (reference.alias != null) return@forEach

            reporter.report(
                RuleDiagnostic(
                    severity = defaultSeverity,
                    message = "FROM and JOIN subqueries should have a table alias.",
                    file = context.file,
                    range = content.rangeAtOffsets(reference.block.startOffset, reference.block.endOffset),
                    database = context.database,
                ),
            )
        }
    }
}
