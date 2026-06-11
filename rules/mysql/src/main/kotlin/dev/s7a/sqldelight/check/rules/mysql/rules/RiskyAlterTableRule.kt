package dev.s7a.sqldelight.check.rules.mysql.rules

import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.DialectCapabilities
import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports MySQL ALTER TABLE statements with operations that commonly rebuild or lock a table.
 */
public class RiskyAlterTableRule : Rule {
    override val id: RuleId = RuleId("mysql:risky-alter-table")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnablement: Enablement = Enablement.Auto

    override fun isApplicable(context: RuleContext): Boolean =
        DialectCapabilities.MySql in context.database.dialect.capabilities

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        if (!isApplicable(context)) return

        val content = context.file.content
        content.sqlTokens()
            .toList()
            .sqlStatements()
            .forEach { statement ->
                val alterTable = statement.alterTableStart() ?: return@forEach
                if (!statement.hasRiskyAlterTableOperation()) return@forEach

                reporter.report(
                    Diagnostic(
                        ruleId = id,
                        severity = defaultSeverity,
                        message =
                            "Review this MySQL ALTER TABLE operation because it can rebuild or strongly lock " +
                                "the table.",
                        file = context.file,
                        range = content.rangeAtOffsets(alterTable.first.startOffset, alterTable.second.endOffset),
                        database = context.database,
                    ),
                )
            }
    }
}

private fun List<SqlToken>.alterTableStart(): Pair<SqlToken, SqlToken>? =
    zipWithNext().firstOrNull { (first, second) ->
        first.isKeyword("alter") && second.isKeyword("table")
    }

private fun List<SqlToken>.hasRiskyAlterTableOperation(): Boolean =
    windowed(size = 2).any { tokens ->
        tokens[0].isKeyword("modify") && tokens[1].isKeyword("column") ||
            tokens[0].isKeyword("change") && tokens[1].isKeyword("column") ||
            tokens[0].isKeyword("drop") && tokens[1].isKeyword("column")
    }
