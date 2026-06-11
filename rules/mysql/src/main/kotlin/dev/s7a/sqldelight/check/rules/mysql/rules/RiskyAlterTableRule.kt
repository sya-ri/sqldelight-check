package dev.s7a.sqldelight.check.rules.mysql.rules

import dev.s7a.sqldelight.check.rule.api.isKeyword
import dev.s7a.sqldelight.check.rule.api.maskSqlCommentsAndQuotedText
import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets
import dev.s7a.sqldelight.check.rule.api.SqlToken
import dev.s7a.sqldelight.check.rule.api.sqlStatements
import dev.s7a.sqldelight.check.rule.api.sqlTokens

import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.DialectCapabilities
import dev.s7a.sqldelight.check.api.DialectCapability
import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports MySQL ALTER TABLE statements with operations that commonly rebuild or lock a table.
 *
 * The rule focuses on migration patterns that can cause long blocking work on
 * live MySQL databases.
 */
public class RiskyAlterTableRule : Rule {
    override val id: RuleId = RuleId("risky-alter-table")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true
    override val targetCapability: DialectCapability = DialectCapabilities.MySql

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        if (!isApplicable(context)) return

        val content = context.file.content
        content.sqlTokens(hashLineComments = true)
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
