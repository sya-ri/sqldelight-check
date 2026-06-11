package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SourceFileKind
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports destructive `DROP TABLE` statements in SQLDelight migration files.
 */
public class NoDropTableInMigrationRule : Rule {
    override val id: RuleId = RuleId("no-drop-table-in-migration")
    override val defaultSeverity: Severity = Severity.Error
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        if (context.file.kind != SourceFileKind.Migration) return

        val content = context.file.content
        val tokens = content.sqlTokens().toList()
        tokens.zipWithNext()
            .filter { (drop, table) ->
                drop.isKeyword("drop") &&
                    table.isKeyword("table") &&
                    content.sqlParenthesisDepthAt(drop.startOffset) == 0
            }
            .forEach { (drop, table) ->
                reporter.report(
                    RuleDiagnostic(
                        severity = defaultSeverity,
                        message = "Avoid DROP TABLE in SQLDelight migrations unless the destructive change is intentional.",
                        file = context.file,
                        range = content.rangeAtOffsets(drop.startOffset, table.endOffset),
                        database = context.database,
                    ),
                )
            }
    }
}
