package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SourceFileKind
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext
import dev.s7a.sqldelight.check.rule.api.SqlToken
import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets
import dev.s7a.sqldelight.check.rule.api.sqlStatements
import dev.s7a.sqldelight.check.rule.api.sqlTokens

/**
 * Reports destructive `DROP COLUMN` operations in SQLDelight migration files.
 */
public class NoDropColumnInMigrationRule : Rule {
    override val id: RuleId = RuleId("no-drop-column-in-migration")
    override val defaultSeverity: Severity = Severity.Error
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        if (context.file.kind != SourceFileKind.Migration) return

        val content = context.file.content
        content.sqlTokens(hashLineComments = true)
            .toList()
            .sqlStatements()
            .mapNotNull { statement -> statement.findAlterTableOperation("drop", "column") }
            .forEach { match ->
                reporter.report(
                    RuleDiagnostic(
                        severity = defaultSeverity,
                        message = "Avoid DROP COLUMN in SQLDelight migrations unless the destructive change is intentional.",
                        file = context.file,
                        range = content.rangeAtOffsets(match.first.startOffset, match.second.endOffset),
                        database = context.database,
                    ),
                )
            }
    }
}

internal fun List<SqlToken>.findAlterTableOperation(
    first: String,
    second: String,
): Pair<SqlToken, SqlToken>? {
    if (!startsWithKeywords("alter", "table")) return null
    return indices.drop(2)
        .zipWithNext()
        .firstOrNull { (left, right) -> get(left).isKeyword(first) && get(right).isKeyword(second) }
        ?.let { (left, right) -> get(left) to get(right) }
}

internal fun List<SqlToken>.startsWithKeywords(vararg keywords: String): Boolean =
    size >= keywords.size && keywords.indices.all { index -> get(index).isKeyword(keywords[index]) }

internal fun SqlToken.isKeyword(value: String): Boolean = text.equals(value, ignoreCase = true)
