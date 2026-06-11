package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports duplicate table aliases within the same SQL statement.
 *
 * Duplicate aliases make qualified column references ambiguous.
 */
public class UniqueTableAliasesRule : Rule {
    override val id: RuleId = RuleId("unique-table-aliases")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        context.file.content
            .tableReferences()
            .filter { reference -> reference.depth == 0 }
            .groupBy { reference -> reference.statementStartOffset }
            .values
            .forEach { references ->
                val seen = mutableSetOf<String>()
                references.mapNotNull { reference -> reference.alias }.forEach { alias ->
                    val normalized = alias.text.lowercase()
                    if (seen.add(normalized)) return@forEach
                    reporter.report(
                        RuleDiagnostic(
                            severity = defaultSeverity,
                            message = "Table aliases should be unique within a statement.",
                            file = context.file,
                            range = context.file.content.rangeAtOffsets(alias.startOffset, alias.endOffset),
                            database = context.database,
                        ),
                    )
                }
            }
    }
}
