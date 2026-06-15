package dev.s7a.sqldelight.check.rules.postgres.rules

import dev.s7a.sqldelight.check.api.DialectId
import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SourceFileKind
import dev.s7a.sqldelight.check.dialects.postgres.PostgresDialectId
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext
import dev.s7a.sqldelight.check.rule.api.SqlToken
import dev.s7a.sqldelight.check.rule.api.isKeyword
import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets
import dev.s7a.sqldelight.check.rule.api.sqlStatements
import dev.s7a.sqldelight.check.rule.api.sqlTokens

/**
 * Reports PostgreSQL ADD COLUMN NOT NULL migrations without a default value.
 */
public class NoAddColumnWithNonNullWithoutDefaultRule : Rule {
    override val id: RuleId = RuleId("no-add-column-with-non-null-without-default")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true
    override val targetDialect: DialectId = PostgresDialectId

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        if (context.file.kind != SourceFileKind.Migration) return

        val content = context.file.content
        content.sqlTokens()
            .toList()
            .sqlStatements()
            .mapNotNull { statement -> statement.findAddColumnNotNullWithoutDefault() }
            .forEach { match ->
                reporter.report(
                    RuleDiagnostic(
                        severity = defaultSeverity,
                        message = "Add nullable columns first, backfill them, then add NOT NULL in a later PostgreSQL migration.",
                        file = context.file,
                        range = content.rangeAtOffsets(match.startOffset, match.endOffset),
                        database = context.database,
                    ),
                )
            }
    }
}

private fun List<SqlToken>.findAddColumnNotNullWithoutDefault(): SqlToken? {
    if (!startsWithKeywords("alter", "table")) return null
    val addIndex = windowed(size = 2, step = 1)
        .indexOfFirst { pair -> pair[0].isKeyword("add") && pair[1].isKeyword("column") }
    if (addIndex == -1) return null
    val columnTokens = drop(addIndex + 2)
    val hasNotNull =
        columnTokens
            .windowed(size = 2, step = 1)
            .any { pair -> pair[0].isKeyword("not") && pair[1].isKeyword("null") }
    val hasDefault = columnTokens.any { token -> token.isKeyword("default") }
    return if (hasNotNull && !hasDefault) get(addIndex) else null
}

private fun List<SqlToken>.startsWithKeywords(vararg keywords: String): Boolean =
    size >= keywords.size && keywords.indices.all { index -> get(index).isKeyword(keywords[index]) }
