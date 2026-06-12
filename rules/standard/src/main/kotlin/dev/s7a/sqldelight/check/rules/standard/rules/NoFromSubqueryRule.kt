package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SqlSourceStructure
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports top-level `FROM` and `JOIN` subqueries that should be written as CTEs.
 */
public class NoFromSubqueryRule : Rule {
    override val id: RuleId = RuleId("no-from-subquery")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        val structure = SqlSourceStructure.parse(content, context.database.dialect.sourcePatterns)
        structure.topLevelSubqueryTableReferences(content, context.database.dialect.sourcePatterns).forEach { reference ->
            reporter.report(
                RuleDiagnostic(
                    severity = defaultSeverity,
                    message = "Use a CTE instead of a top-level FROM or JOIN subquery.",
                    file = context.file,
                    range = content.rangeAtOffsets(reference.block.startOffset, reference.block.endOffset),
                    database = context.database,
                ),
            )
        }
    }
}
